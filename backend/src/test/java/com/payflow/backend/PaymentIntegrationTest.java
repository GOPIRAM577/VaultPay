package com.payflow.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payflow.backend.dto.PaymentRequest;
import com.payflow.backend.entity.*;
import com.payflow.backend.repository.OutboxEventRepository;
import com.payflow.backend.repository.PaymentRepository;
import com.payflow.backend.repository.UserRepository;
import com.payflow.backend.repository.WalletRepository;
import com.payflow.backend.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

class PaymentIntegrationTest extends AbstractIntegrationTest {

    @LocalServerPort
    private int port;

    private final RestTemplate restTemplate = new RestTemplate();

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        outboxEventRepository.deleteAll();
        paymentRepository.deleteAll();
        walletRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void shouldProcessPaymentAndCreateOutboxEvent() throws Exception {
        // Setup Sender
        User sender = User.builder()
                .name("Sender")
                .email("sender@test.com")
                .phone("1234567890")
                .passwordHash("hash")
                .role(Role.USER)
                .isActive(true)
                .build();
        sender = userRepository.save(sender);

        Wallet senderWallet = Wallet.builder()
                .userId(sender.getId())
                .balance(new BigDecimal("1000.00"))
                .currency("INR")
                .status(WalletStatus.ACTIVE)
                .build();
        senderWallet = walletRepository.save(senderWallet);

        // Setup Receiver
        User receiver = User.builder()
                .name("Receiver")
                .email("receiver@test.com")
                .phone("0987654321")
                .passwordHash("hash")
                .role(Role.USER)
                .isActive(true)
                .build();
        receiver = userRepository.save(receiver);

        Wallet receiverWallet = Wallet.builder()
                .userId(receiver.getId())
                .balance(new BigDecimal("0.00"))
                .currency("INR")
                .status(WalletStatus.ACTIVE)
                .build();
        receiverWallet = walletRepository.save(receiverWallet);

        // Authenticate
        String token = jwtService.generateToken(sender.getId(), sender.getEmail(), sender.getRole().name());

        PaymentRequest request = new PaymentRequest();
        request.setReceiverWalletId(receiverWallet.getId());
        request.setAmount(new BigDecimal("100.00"));

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Idempotency-Key", UUID.randomUUID().toString());

        HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(request), headers);

        // When
        ResponseEntity<Payment> response = restTemplate.postForEntity("http://localhost:" + port + "/payments/initiate", entity, Payment.class);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        Payment initiatedPayment = response.getBody();
        assertThat(initiatedPayment).isNotNull();

        // Wait for payment to reach COMPLETED status
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            Payment payment = paymentRepository.findById(initiatedPayment.getId()).orElseThrow();
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        });

        // Verify outbox event creation and processing
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            List<OutboxEvent> events = outboxEventRepository.findAll();
            assertThat(events).hasSize(1);
            OutboxEvent outboxEvent = events.get(0);
            assertThat(outboxEvent.getEventType()).isEqualTo("PAYMENT_COMPLETED");
            assertThat(outboxEvent.getPayload()).contains(initiatedPayment.getId().toString());
            assertThat(outboxEvent.getStatus()).isEqualTo("PROCESSED");
        });
    }
}
