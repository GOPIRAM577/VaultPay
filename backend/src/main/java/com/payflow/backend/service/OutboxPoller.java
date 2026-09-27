package com.payflow.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.payflow.backend.entity.OutboxEvent;
import com.payflow.backend.entity.Payment;
import com.payflow.backend.entity.User;
import com.payflow.backend.repository.OutboxEventRepository;
import com.payflow.backend.repository.PaymentRepository;
import com.payflow.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxPoller {

    private final OutboxEventRepository outboxEventRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final ResendEmailClient resendEmailClient;
    private final EmailTemplateEngine emailTemplateEngine;
    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate messagingTemplate;

    @Scheduled(fixedDelay = 2000)
    @Transactional
    public void pollOutbox() {
        List<OutboxEvent> pendingEvents = outboxEventRepository.findByStatusOrderByCreatedAtAsc("PENDING");
        
        for (OutboxEvent event : pendingEvents) {
            try {
                if (event.getEventType().startsWith("PAYMENT_")) {
                    JsonNode payload = objectMapper.readTree(event.getPayload());
                    String paymentIdStr = payload.get("paymentId").asText();
                    UUID paymentId = UUID.fromString(paymentIdStr);
                    String status = event.getEventType().substring("PAYMENT_".length());
                    
                    // 1. Publish WebSocket Update
                    Object wsPayload = java.util.Map.of("paymentId", paymentIdStr, "status", status);
                    messagingTemplate.convertAndSend("/topic/payments/" + paymentIdStr, wsPayload);
                    
                    // 2. Handle specific email notifications
                    if ("COMPLETED".equals(status) || "FAILED".equals(status)) {
                        Payment payment = paymentRepository.findById(paymentId)
                                .orElseThrow(() -> new RuntimeException("Payment not found"));
                                
                        User sender = userRepository.findById(payment.getSenderUserId())
                                .orElseThrow(() -> new RuntimeException("Sender not found"));
                                
                        User receiver = userRepository.findById(payment.getReceiverUserId())
                                .orElseThrow(() -> new RuntimeException("Receiver not found"));

                        if ("COMPLETED".equals(status)) {
                            try {
                                String senderHtml = emailTemplateEngine.generatePaymentCompletedSenderHtml(payment, receiver.getName());
                                resendEmailClient.sendEmail(sender.getEmail(), "Payment Completed", senderHtml);
                            } catch (Exception e) {
                                log.error("Failed to send sender completion email to {}: {}", sender.getEmail(), e.getMessage());
                            }
                            
                            try {
                                String receiverHtml = emailTemplateEngine.generatePaymentCompletedReceiverHtml(payment, sender.getName());
                                resendEmailClient.sendEmail(receiver.getEmail(), "Payment Received", receiverHtml);
                            } catch (Exception e) {
                                log.error("Failed to send receiver completion email to {}: {}", receiver.getEmail(), e.getMessage());
                            }
                        } else if ("FAILED".equals(status)) {
                            try {
                                String failedHtml = emailTemplateEngine.generatePaymentFailedHtml(payment, receiver.getName());
                                resendEmailClient.sendEmail(sender.getEmail(), "Payment Failed", failedHtml);
                            } catch (Exception e) {
                                log.error("Failed to send payment failure email to {}: {}", sender.getEmail(), e.getMessage());
                            }
                        }
                    }
                }
                
                // On success
                event.setStatus("PROCESSED");
                event.setProcessedAt(LocalDateTime.now());
                log.info("Successfully processed outbox event: {}", event.getId());
                
            } catch (Exception e) {
                log.error("Failed to process outbox event: {}", event.getId(), e);
                event.setRetryCount(event.getRetryCount() + 1);
                
                if (event.getRetryCount() >= 3) {
                    event.setStatus("FAILED");
                    log.error("Outbox event {} permanently failed after 3 retries", event.getId());
                }
            } finally {
                outboxEventRepository.save(event);
            }
        }
    }
}
