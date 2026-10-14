package com.payflow.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "settlement_reports")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SettlementReport {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "report_date", nullable = false)
    private LocalDate reportDate;

    @Column(name = "wallet_id", nullable = false)
    private UUID walletId;

    @Column(name = "total_credits", nullable = false)
    private BigDecimal totalCredits;

    @Column(name = "total_debits", nullable = false)
    private BigDecimal totalDebits;

    @Column(name = "net_position", nullable = false)
    private BigDecimal netPosition;

    @CreationTimestamp
    @Column(name = "generated_at", updatable = false)
    private LocalDateTime generatedAt;
}
