package com.payflow.backend.repository;

import com.payflow.backend.entity.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    Page<Payment> findBySenderUserIdOrReceiverUserIdOrderByCreatedAtDesc(UUID senderUserId, UUID receiverUserId, Pageable pageable);

    @Query(value = "SELECT COUNT(*) FROM payments WHERE sender_user_id = :senderId AND created_at > NOW() - INTERVAL '1 hour'", nativeQuery = true)
    long countVelocityRule(@Param("senderId") UUID senderId);

    long countBySenderUserIdAndReceiverUserId(UUID senderUserId, UUID receiverUserId);

    Page<Payment> findByStatusIn(java.util.List<com.payflow.backend.entity.PaymentStatus> statuses, Pageable pageable);
}
