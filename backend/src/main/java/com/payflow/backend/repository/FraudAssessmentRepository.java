package com.payflow.backend.repository;

import com.payflow.backend.entity.FraudAssessment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

import java.util.Optional;

public interface FraudAssessmentRepository extends JpaRepository<FraudAssessment, UUID> {
    Optional<FraudAssessment> findByPaymentId(UUID paymentId);
}
