package com.payflow.backend.service;

import com.payflow.backend.dto.PaymentRequest;
import com.payflow.backend.entity.FraudAssessment;
import com.payflow.backend.entity.FraudDecision;
import com.payflow.backend.entity.Payment;
import com.payflow.backend.entity.Wallet;
import com.payflow.backend.repository.PaymentRepository;
import com.payflow.backend.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class FraudScoringEngineTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private WalletRepository walletRepository;

    @InjectMocks
    private FraudScoringEngine fraudScoringEngine;

    private UUID senderUserId;
    private UUID receiverWalletId;
    private UUID receiverUserId;
    private UUID paymentId;
    private Payment payment;
    private Wallet senderWallet;
    private Wallet receiverWallet;

    @BeforeEach
    void setUp() {
        senderUserId = UUID.randomUUID();
        receiverWalletId = UUID.randomUUID();
        receiverUserId = UUID.randomUUID();
        paymentId = UUID.randomUUID();

        payment = new Payment();
        payment.setId(paymentId);
        payment.setAmount(new BigDecimal("100.00"));
        payment.setReceiverWalletId(receiverWalletId);

        senderWallet = new Wallet();
        senderWallet.setUserId(senderUserId);
        senderWallet.setCreatedAt(LocalDateTime.now().minusDays(5)); // Old wallet (no trigger)

        receiverWallet = new Wallet();
        receiverWallet.setId(receiverWalletId);
        receiverWallet.setUserId(receiverUserId);
        
        when(walletRepository.findByUserId(senderUserId)).thenReturn(Optional.of(senderWallet));
        when(walletRepository.findById(receiverWalletId)).thenReturn(Optional.of(receiverWallet));
        when(paymentRepository.countVelocityRule(senderUserId)).thenReturn(2L); // Under 5
        when(paymentRepository.countBySenderUserIdAndReceiverUserId(senderUserId, receiverUserId)).thenReturn(5L); // Has previous
    }

    @Test
    void testAllSafe_ShouldApprove() {
        FraudAssessment assessment = fraudScoringEngine.computeScore(payment, senderUserId);
        
        // At most, TimeOfDay rule might trigger if run between 1-5 AM (0.15)
        // Score will be <= 0.15, which is < 0.4 (APPROVED)
        assertThat(assessment.getDecision()).isEqualTo(FraudDecision.APPROVED);
        assertThat(assessment.getRiskScore()).isLessThan(new BigDecimal("0.4"));
    }

    @Test
    void testVelocityRule_ShouldTrigger() {
        when(paymentRepository.countVelocityRule(senderUserId)).thenReturn(6L); // > 5

        FraudAssessment assessment = fraudScoringEngine.computeScore(payment, senderUserId);
        
        assertThat(assessment.getRulesTriggered()).anyMatch(r -> r.getRuleName().equals("VelocityRule") && r.isTriggered());
    }

    @Test
    void testAmountRule_ShouldTrigger() {
        payment.setAmount(new BigDecimal("6000.00")); // > 5000

        FraudAssessment assessment = fraudScoringEngine.computeScore(payment, senderUserId);
        
        assertThat(assessment.getRulesTriggered()).anyMatch(r -> r.getRuleName().equals("AmountRule") && r.isTriggered());
    }

    @Test
    void testWalletAgeRule_ShouldTrigger() {
        senderWallet.setCreatedAt(LocalDateTime.now().minusHours(2)); // < 1 day old

        FraudAssessment assessment = fraudScoringEngine.computeScore(payment, senderUserId);
        
        assertThat(assessment.getRulesTriggered()).anyMatch(r -> r.getRuleName().equals("WalletAgeRule") && r.isTriggered());
    }

    @Test
    void testRecipientRelationshipRule_ShouldTrigger() {
        when(paymentRepository.countBySenderUserIdAndReceiverUserId(senderUserId, receiverUserId)).thenReturn(0L); // 0 previous sends

        FraudAssessment assessment = fraudScoringEngine.computeScore(payment, senderUserId);
        
        assertThat(assessment.getRulesTriggered()).anyMatch(r -> r.getRuleName().equals("RecipientRelationshipRule") && r.isTriggered());
    }

    @Test
    void testHighFraud_ShouldReject() {
        // Trigger all possible rules
        when(paymentRepository.countVelocityRule(senderUserId)).thenReturn(10L); // 0.3
        payment.setAmount(new BigDecimal("10000.00")); // 0.2
        senderWallet.setCreatedAt(LocalDateTime.now().minusHours(1)); // 0.2
        when(paymentRepository.countBySenderUserIdAndReceiverUserId(senderUserId, receiverUserId)).thenReturn(0L); // 0.1

        FraudAssessment assessment = fraudScoringEngine.computeScore(payment, senderUserId);
        
        // Total score without TimeOfDay is 0.8
        assertThat(assessment.getRiskScore()).isGreaterThanOrEqualTo(new BigDecimal("0.7"));
        assertThat(assessment.getDecision()).isEqualTo(FraudDecision.REJECTED);
    }
}
