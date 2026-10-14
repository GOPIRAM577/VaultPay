package com.payflow.backend.service;

import com.payflow.backend.dto.PaymentRequest;
import com.payflow.backend.entity.FraudAssessment;
import com.payflow.backend.entity.FraudDecision;
import com.payflow.backend.entity.RuleTrigger;
import com.payflow.backend.entity.Wallet;
import com.payflow.backend.repository.PaymentRepository;
import com.payflow.backend.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.payflow.backend.entity.Payment;

@Service
@RequiredArgsConstructor
public class FraudScoringEngine {

    private final PaymentRepository paymentRepository;
    private final WalletRepository walletRepository;

    public FraudAssessment computeScore(Payment payment, UUID senderUserId) {
        List<RuleTrigger> rules = new ArrayList<>();
        double totalScore = 0.0;

        // 1. Velocity rule (more than 5 transactions in 1 hour)
        long recentPayments = paymentRepository.countVelocityRule(senderUserId);
        boolean velocityTriggered = recentPayments > 5;
        double velocityScore = velocityTriggered ? 0.3 : 0.0;
        totalScore += velocityScore;
        rules.add(new RuleTrigger("VelocityRule", velocityScore, velocityTriggered));

        // 2. Amount rule (> 5000)
        boolean amountTriggered = payment.getAmount().compareTo(new BigDecimal("5000")) > 0;
        double amountScore = amountTriggered ? 0.2 : 0.0;
        totalScore += amountScore;
        rules.add(new RuleTrigger("AmountRule", amountScore, amountTriggered));

        // 3. Wallet age rule (newer than 1 day)
        Wallet senderWallet = walletRepository.findByUserId(senderUserId).orElseThrow();
        boolean ageTriggered = senderWallet.getCreatedAt().isAfter(LocalDateTime.now().minusDays(1));
        double ageScore = ageTriggered ? 0.2 : 0.0;
        totalScore += ageScore;
        rules.add(new RuleTrigger("WalletAgeRule", ageScore, ageTriggered));

        // 4. Recipient relationship rule (0 previous transfers)
        Wallet receiverWallet = walletRepository.findById(payment.getReceiverWalletId()).orElseThrow();
        long previousSends = paymentRepository.countBySenderUserIdAndReceiverUserId(senderUserId, receiverWallet.getUserId());
        boolean relationshipTriggered = previousSends == 0;
        double relationshipScore = relationshipTriggered ? 0.1 : 0.0;
        totalScore += relationshipScore;
        rules.add(new RuleTrigger("RecipientRelationshipRule", relationshipScore, relationshipTriggered));

        // 5. Time of day rule (between 1 AM and 5 AM)
        int hour = LocalDateTime.now().getHour();
        boolean timeTriggered = hour >= 1 && hour <= 5;
        double timeScore = timeTriggered ? 0.15 : 0.0;
        totalScore += timeScore;
        rules.add(new RuleTrigger("TimeOfDayRule", timeScore, timeTriggered));

        // Cap at 1.0
        totalScore = Math.min(totalScore, 1.0);

        FraudDecision decision;
        if (totalScore >= 0.7) {
            decision = FraudDecision.REJECTED;
        } else if (totalScore >= 0.4) {
            decision = FraudDecision.FLAGGED;
        } else {
            decision = FraudDecision.APPROVED;
        }

        return FraudAssessment.builder()
                .paymentId(payment.getId())
                .riskScore(BigDecimal.valueOf(totalScore))
                .decision(decision)
                .rulesTriggered(rules)
                .build();
    }
}
