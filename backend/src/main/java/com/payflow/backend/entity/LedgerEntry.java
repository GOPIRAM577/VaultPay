package com.payflow.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "ledger_entries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LedgerEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "payment_id")
    private UUID paymentId;

    @Column(nullable = false)
    private UUID walletId;

    @Column(name = "seq", nullable = false, insertable = false, updatable = false)
    @org.hibernate.annotations.Generated
    private Long seq;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LedgerEntryType entryType;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    private String description;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    @PreUpdate
    private void normalizeAmount() {
        if (amount != null && entryType != null) {
            if (entryType == LedgerEntryType.DEBIT) {
                amount = amount.abs().negate();
            } else if (entryType == LedgerEntryType.CREDIT) {
                amount = amount.abs();
            }
        }
    }
}
