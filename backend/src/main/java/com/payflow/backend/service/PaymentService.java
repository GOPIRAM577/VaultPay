package com.payflow.backend.service;

import com.payflow.backend.entity.Payment;
import com.payflow.backend.entity.PaymentStatus;
import com.payflow.backend.entity.Wallet;
import com.payflow.backend.entity.WalletStatus;
import com.payflow.backend.entity.FraudAssessment;
import com.payflow.backend.entity.FraudDecision;
import com.payflow.backend.entity.OutboxEvent;
import com.payflow.backend.repository.FraudAssessmentRepository;
import com.payflow.backend.repository.PaymentRepository;
import com.payflow.backend.repository.WalletRepository;
import com.payflow.backend.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final WalletRepository walletRepository;
    private final LedgerService ledgerService;
    private final WalletCacheService walletCacheService;
    private final PaymentRepository paymentRepository;
    private final PaymentStateMachine stateMachine;
    private final GatewayService gatewayService;
    private final GroqNarrativeClient groqNarrativeClient;
    private final FraudAssessmentRepository fraudAssessmentRepository;
    private final OutboxEventRepository outboxEventRepository;

    @Transactional
    public Payment initiatePayment(UUID senderUserId, UUID receiverWalletId, BigDecimal amount, String idempotencyKey) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0 || amount.compareTo(new BigDecimal("10000")) > 0) {
            throw new IllegalArgumentException("Amount must be between 0 and 10000");
        }
        
        Wallet senderWallet = walletRepository.findByUserId(senderUserId)
                .orElseThrow(() -> new RuntimeException("Sender wallet not found"));
                
        if (senderWallet.getId().equals(receiverWalletId)) {
            throw new IllegalArgumentException("Cannot send money to yourself.");
        }

        if (senderWallet.getStatus() == WalletStatus.FROZEN) {
            throw new IllegalStateException("Sender wallet is frozen");
        }

        Wallet receiverWallet = walletRepository.findById(receiverWalletId)
                .orElseThrow(() -> new RuntimeException("Receiver wallet not found"));

        if (receiverWallet.getStatus() == WalletStatus.FROZEN) {
            throw new IllegalStateException("Receiver wallet is frozen");
        }

        Payment payment = Payment.builder()
                .senderUserId(senderUserId)
                .receiverWalletId(receiverWalletId)
                .receiverUserId(receiverWallet.getUserId())
                .amount(amount)
                .status(PaymentStatus.INITIATED)
                .idempotencyKey(idempotencyKey)
                .build();
                
        return paymentRepository.save(payment);
    }

    @Transactional
    public void process(UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        try {
            stateMachine.transition(payment, PaymentStatus.FRAUD_REVIEW);
            paymentRepository.save(payment);
            
            outboxEventRepository.save(OutboxEvent.builder()
                    .eventType("PAYMENT_FRAUD_REVIEW")
                    .payload(String.format("{\"paymentId\": \"%s\"}", payment.getId()))
                    .status("PENDING")
                    .build());
            
            BigDecimal fraudScore = payment.getFraudScore() != null ? payment.getFraudScore() : calculateFraudScore(payment);
            
            if (fraudScore.compareTo(new BigDecimal("0.70")) >= 0) {
                // High risk score -> REJECTED
                stateMachine.transition(payment, PaymentStatus.REJECTED);
                paymentRepository.save(payment);
                
                outboxEventRepository.save(OutboxEvent.builder()
                        .eventType("PAYMENT_REJECTED")
                        .payload(String.format("{\"paymentId\": \"%s\"}", payment.getId()))
                        .status("PENDING")
                        .build());
                
                FraudAssessment assessment = FraudAssessment.builder()
                        .paymentId(payment.getId())
                        .riskScore(fraudScore)
                        .rulesTriggered(java.util.Collections.emptyList())
                        .decision(FraudDecision.REJECTED)
                        .aiNarrative("Transaction blocked due to excessively high fraud score.")
                        .build();
                fraudAssessmentRepository.save(assessment);
                
                log.warn("Payment {} rejected due to high fraud score: {}", payment.getId(), fraudScore);
                return; // Stop processing further
            } else if (fraudScore.compareTo(new BigDecimal("0.4")) >= 0 && fraudScore.compareTo(new BigDecimal("0.69")) <= 0) {
                // Borderline score
                stateMachine.transition(payment, PaymentStatus.PROCESSING);
                paymentRepository.save(payment);
                
                outboxEventRepository.save(OutboxEvent.builder()
                        .eventType("PAYMENT_PROCESSING")
                        .payload(String.format("{\"paymentId\": \"%s\"}", payment.getId()))
                        .status("PENDING")
                        .build());
                
                FraudAssessment assessment = FraudAssessment.builder()
                        .paymentId(payment.getId())
                        .riskScore(BigDecimal.valueOf(fraudScore.doubleValue()))
                        .rulesTriggered(java.util.Collections.emptyList())
                        .decision(FraudDecision.FLAGGED)
                        .build();
                final FraudAssessment savedAssessment = fraudAssessmentRepository.save(assessment);
                final java.math.BigDecimal paymentAmount = payment.getAmount();
                final UUID assessmentId = savedAssessment.getId();
                
                org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                    new org.springframework.transaction.support.TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            groqNarrativeClient.generateAndStoreNarrative(paymentAmount, assessmentId);
                        }
                    }
                );
            } else {
                // Assume fraud review passes
                stateMachine.transition(payment, PaymentStatus.PROCESSING);
                paymentRepository.save(payment);
                
                outboxEventRepository.save(OutboxEvent.builder()
                        .eventType("PAYMENT_PROCESSING")
                        .payload(String.format("{\"paymentId\": \"%s\"}", payment.getId()))
                        .status("PENDING")
                        .build());
            }

            // Lock sender wallet
            Wallet lockedSenderWallet = walletRepository.findByIdForUpdate(
                    walletRepository.findByUserId(payment.getSenderUserId())
                            .orElseThrow(() -> new RuntimeException("Sender wallet not found")).getId())
                    .orElseThrow(() -> new RuntimeException("Sender wallet not found"));

            if (lockedSenderWallet.getBalance().compareTo(payment.getAmount()) < 0) {
                stateMachine.transition(payment, PaymentStatus.FAILED);
                paymentRepository.save(payment);
                
                OutboxEvent failedEvent = OutboxEvent.builder()
                        .eventType("PAYMENT_FAILED")
                        .payload(String.format("{\"paymentId\": \"%s\"}", payment.getId()))
                        .status("PENDING")
                        .build();
                outboxEventRepository.save(failedEvent);
                
                return;
            }

            // Lock receiver wallet
            Wallet lockedReceiverWallet = walletRepository.findByIdForUpdate(payment.getReceiverWalletId())
                    .orElseThrow(() -> new RuntimeException("Receiver wallet not found"));

            // Initial transfer (Debit Sender, Credit Receiver)
            lockedSenderWallet.setBalance(lockedSenderWallet.getBalance().subtract(payment.getAmount()));
            lockedReceiverWallet.setBalance(lockedReceiverWallet.getBalance().add(payment.getAmount()));

            walletRepository.save(lockedSenderWallet);
            walletRepository.save(lockedReceiverWallet);
            ledgerService.writeDoubleEntry(payment.getId(), lockedSenderWallet.getId(), lockedReceiverWallet.getId(), payment.getAmount());

            // Simulate Gateway Capture while in PROCESSING state
            GatewayResponse gatewayResponse = gatewayService.simulateCapture();
            
            if (gatewayResponse == GatewayResponse.GATEWAY_DECLINED) {
                // Compensating Transaction
                if (payment.getStatus() == PaymentStatus.PROCESSING) {
                    lockedSenderWallet.setBalance(lockedSenderWallet.getBalance().add(payment.getAmount()));
                    lockedReceiverWallet.setBalance(lockedReceiverWallet.getBalance().subtract(payment.getAmount()));
                    
                    walletRepository.save(lockedSenderWallet);
                    walletRepository.save(lockedReceiverWallet);
                    
                    // Compensating ledger entries (Sender is credited, Receiver is debited)
                    // The original writeDoubleEntry debits the first wallet and credits the second.
                    // So we pass receiver as the first (to debit) and sender as the second (to credit).
                    ledgerService.writeDoubleEntry(payment.getId(), lockedReceiverWallet.getId(), lockedSenderWallet.getId(), payment.getAmount());
                    
                    stateMachine.transition(payment, PaymentStatus.FAILED);
                    paymentRepository.save(payment);
                    
                    OutboxEvent failedEvent = OutboxEvent.builder()
                            .eventType("PAYMENT_FAILED")
                            .payload(String.format("{\"paymentId\": \"%s\"}", payment.getId()))
                            .status("PENDING")
                            .build();
                    outboxEventRepository.save(failedEvent);
                    
                    walletCacheService.invalidateBalanceCache(lockedSenderWallet.getId());
                    walletCacheService.invalidateBalanceCache(lockedReceiverWallet.getId());
                    log.warn("Payment {} failed at gateway. Compensating transaction applied.", payment.getId());
                    return;
                }
            }

            // Success
            walletCacheService.invalidateBalanceCache(lockedSenderWallet.getId());
            walletCacheService.invalidateBalanceCache(lockedReceiverWallet.getId());

            stateMachine.transition(payment, PaymentStatus.COMPLETED);
            paymentRepository.save(payment);
            
            // Create outbox event
            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .eventType("PAYMENT_COMPLETED")
                    .payload(String.format("{\"paymentId\": \"%s\"}", payment.getId()))
                    .status("PENDING")
                    .build();
            outboxEventRepository.save(outboxEvent);
            
            log.info("Payment {} successful for amount {}", payment.getId(), payment.getAmount());
            
        } catch (Exception e) {
            log.error("Payment {} failed during processing: {}", payment.getId(), e.getMessage());
            // Optionally, we could set status to FAILED here in a new transaction
            throw e;
        }
    }

    private BigDecimal calculateFraudScore(Payment payment) {
        BigDecimal score = BigDecimal.ZERO;
        BigDecimal amount = payment.getAmount();

        // 1. Amount Rule
        if (amount.compareTo(new BigDecimal("100")) <= 0) {
            score = score.add(new BigDecimal("0.10"));
        } else if (amount.compareTo(new BigDecimal("5000")) <= 0) {
            score = score.add(new BigDecimal("0.30"));
        } else {
            score = score.add(new BigDecimal("0.60"));
        }

        // 2. Wallet Age Rule
        Wallet senderWallet = walletRepository.findByUserId(payment.getSenderUserId())
                .orElseThrow(() -> new RuntimeException("Sender wallet not found"));
        
        if (senderWallet.getCreatedAt() != null && senderWallet.getCreatedAt().isAfter(java.time.LocalDateTime.now().minusHours(24))) {
            score = score.add(new BigDecimal("0.25")); // High risk for new wallets
        }

        // 3. Velocity Rule
        long recentTransactions = paymentRepository.countVelocityRule(payment.getSenderUserId());
        if (recentTransactions > 5) {
            score = score.add(new BigDecimal("0.20"));
        }

        // 4. Transaction History Rule
        if (payment.getReceiverUserId() != null) {
            long history = paymentRepository.countBySenderUserIdAndReceiverUserId(payment.getSenderUserId(), payment.getReceiverUserId());
            if (history > 0) {
                score = score.subtract(new BigDecimal("0.20")); // Trust built
            }
        }

        // Bound between 0.01 and 0.99
        if (score.compareTo(new BigDecimal("0.01")) < 0) {
            score = new BigDecimal("0.01");
        } else if (score.compareTo(new BigDecimal("0.99")) > 0) {
            score = new BigDecimal("0.99");
        }
        
        return score;
    }
}
