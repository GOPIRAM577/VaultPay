package com.payflow.backend.controller;

import com.payflow.backend.dto.PaymentRequest;
import com.payflow.backend.service.PaymentService;
import com.payflow.backend.entity.Payment;
import com.payflow.backend.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final PaymentRepository paymentRepository;

    @PostMapping("/initiate")
    public ResponseEntity<Payment> initiatePayment(
            Authentication authentication, 
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody PaymentRequest request) {
        UUID senderUserId = UUID.fromString((String) authentication.getPrincipal());
        Payment payment = paymentService.initiatePayment(senderUserId, request.getReceiverWalletId(), request.getAmount(), idempotencyKey);
        
        // Triggers async processing if we had an async event, but for now we'll just return the INITIATED payment
        // Wait, the prompt says "Wire GatewayService.simulateCapture() into PaymentService.process() so ~10% of payments naturally fail"
        // And "POST /payments/initiate to create a Payment with status INITIATED and return immediately"
        // So we will just call process asynchronously in a real app, but here we can just spin up a new thread for demonstration,
        // or just return and assume a worker processes it. Let's start a thread to process it async.
        new Thread(() -> paymentService.process(payment.getId())).start();

        return ResponseEntity.status(201).body(payment);
    }

    @GetMapping("/{id}/status")
    public ResponseEntity<Map<String, String>> getPaymentStatus(@PathVariable UUID id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payment not found"));
        return ResponseEntity.ok(Map.of("status", payment.getStatus().name()));
    }

    @GetMapping("/me/history")
    public ResponseEntity<Page<Payment>> getPaymentHistory(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
            
        UUID userId = UUID.fromString((String) authentication.getPrincipal());
        Page<Payment> history = paymentRepository.findBySenderUserIdOrReceiverUserIdOrderByCreatedAtDesc(
                userId, userId, PageRequest.of(page, size));
        return ResponseEntity.ok(history);
    }
}
