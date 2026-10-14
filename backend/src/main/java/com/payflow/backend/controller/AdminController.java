package com.payflow.backend.controller;

import com.payflow.backend.entity.AuditLog;
import com.payflow.backend.entity.User;
import com.payflow.backend.entity.Wallet;
import com.payflow.backend.entity.WalletStatus;
import com.payflow.backend.entity.SettlementReport;
import com.payflow.backend.entity.ReconciliationAlert;
import com.payflow.backend.repository.AuditLogRepository;
import com.payflow.backend.repository.UserRepository;
import com.payflow.backend.repository.WalletRepository;
import com.payflow.backend.repository.SettlementReportRepository;
import com.payflow.backend.repository.ReconciliationAlertRepository;
import com.payflow.backend.repository.OutboxEventRepository;
import com.payflow.backend.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.PrintWriter;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import com.payflow.backend.entity.Payment;
import com.payflow.backend.entity.PaymentStatus;
import com.payflow.backend.repository.PaymentRepository;
import com.payflow.backend.repository.FraudAssessmentRepository;
import com.payflow.backend.dto.FraudAlertDTO;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final WalletRepository walletRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;
    private final AuditLogRepository auditLogRepository;
    private final SettlementReportRepository settlementReportRepository;
    private final ReconciliationAlertRepository reconciliationAlertRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final PaymentRepository paymentRepository;
    private final FraudAssessmentRepository fraudAssessmentRepository;

    @GetMapping("/fraud/alerts")
    public ResponseEntity<Page<FraudAlertDTO>> getFraudAlerts(Pageable pageable) {
        Page<Payment> flaggedPayments = paymentRepository.findByStatusIn(
                List.of(PaymentStatus.REJECTED, PaymentStatus.FRAUD_REVIEW), pageable);

        Page<FraudAlertDTO> alerts = flaggedPayments.map(payment -> {
            return FraudAlertDTO.builder()
                    .payment(payment)
                    .fraudAssessment(fraudAssessmentRepository.findByPaymentId(payment.getId()).orElse(null))
                    .build();
        });

        return ResponseEntity.ok(alerts);
    }

    @GetMapping("/users")
    public ResponseEntity<Page<User>> getUsers(Pageable pageable) {
        return ResponseEntity.ok(userRepository.findAll(pageable));
    }

    @GetMapping("/wallets")
    public ResponseEntity<Page<Wallet>> getWallets(Pageable pageable) {
        return ResponseEntity.ok(walletRepository.findAll(pageable));
    }

    @PatchMapping("/wallets/{walletId}/status")
    public ResponseEntity<Wallet> toggleWalletStatus(@PathVariable UUID walletId, Authentication authentication) {
        UUID adminId = UUID.fromString((String) authentication.getPrincipal());
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new RuntimeException("Wallet not found"));
        
        WalletStatus previousStatus = wallet.getStatus();
        WalletStatus newStatus = (previousStatus == WalletStatus.ACTIVE) ? WalletStatus.FROZEN : WalletStatus.ACTIVE;
        wallet.setStatus(newStatus);
        walletRepository.save(wallet);

        auditLogService.log(adminId, "TOGGLE_WALLET_STATUS", "WALLET", walletId, 
                Map.of("previousStatus", previousStatus.name(), "newStatus", newStatus.name()));
        return ResponseEntity.ok(wallet);
    }

    @PatchMapping("/users/{userId}/activate")
    public ResponseEntity<User> toggleUserActivation(@PathVariable UUID userId, Authentication authentication) {
        UUID adminId = UUID.fromString((String) authentication.getPrincipal());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        boolean previousStatus = user.getIsActive() != null ? user.getIsActive() : true;
        boolean newStatus = !previousStatus;
        user.setIsActive(newStatus);
        userRepository.save(user);

        auditLogService.log(adminId, "TOGGLE_USER_ACTIVATION", "USER", userId, 
                Map.of("previousStatus", previousStatus, "newStatus", newStatus));
        return ResponseEntity.ok(user);
    }

    @GetMapping("/audit-logs")
    public ResponseEntity<Page<AuditLog>> getAuditLogs(Pageable pageable) {
        return ResponseEntity.ok(auditLogRepository.findAll(pageable));
    }
    
    @GetMapping("/settlements/{date}/report")
    public ResponseEntity<List<SettlementReport>> getSettlementReport(@PathVariable String date) {
        LocalDate parsedDate = LocalDate.parse(date);
        return ResponseEntity.ok(settlementReportRepository.findByReportDate(parsedDate));
    }

    @GetMapping("/settlements/{date}/report.csv")
    public ResponseEntity<StreamingResponseBody> getSettlementCsv(@PathVariable String date) {
        LocalDate parsedDate = LocalDate.parse(date);
        List<SettlementReport> reports = settlementReportRepository.findByReportDate(parsedDate);
        
        StreamingResponseBody stream = out -> {
            try (PrintWriter writer = new PrintWriter(out)) {
                writer.println("WalletId,TotalCredits,TotalDebits,NetPosition");
                for (SettlementReport report : reports) {
                    writer.printf("%s,%s,%s,%s\n", 
                            report.getWalletId(), 
                            report.getTotalCredits(), 
                            report.getTotalDebits(), 
                            report.getNetPosition());
                }
            }
        };

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=settlement_" + date + ".csv")
                .header(HttpHeaders.CONTENT_TYPE, "text/csv")
                .body(stream);
    }

    @GetMapping("/reconciliation")
    public ResponseEntity<Map<String, Object>> getReconciliationSummary() {
        Map<String, Object> response = new HashMap<>();
        response.put("lastRunAt", reconciliationAlertRepository.findLastRunAt());
        response.put("mismatchCount", reconciliationAlertRepository.countByResolvedAtIsNull());
        response.put("alerts", reconciliationAlertRepository.findAll()); // Simple approach
        return ResponseEntity.ok(response);
    }

    @GetMapping("/outbox/stats")
    public ResponseEntity<Map<String, Long>> getOutboxStats() {
        List<Object[]> results = outboxEventRepository.countByStatus();
        Map<String, Long> stats = new HashMap<>();
        stats.put("pending", 0L);
        stats.put("processed", 0L);
        stats.put("failed", 0L);
        
        for (Object[] row : results) {
            String status = (String) row[0];
            Long count = ((Number) row[1]).longValue();
            stats.put(status.toLowerCase(), count);
        }
        
        return ResponseEntity.ok(stats);
    }
}
