package com.payflow.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "fraud_assessments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FraudAssessment {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "payment_id", nullable = false)
    private UUID paymentId;

    @Column(name = "risk_score", nullable = false)
    private BigDecimal riskScore;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private FraudDecision decision;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "rules_triggered", columnDefinition = "jsonb")
    private List<RuleTrigger> rulesTriggered;

    @Column(name = "ai_narrative")
    private String aiNarrative;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
