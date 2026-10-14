package com.payflow.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import jakarta.validation.constraints.Pattern;

@Entity
@Table(name = "wallets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private UUID userId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WalletStatus status;

    @Version
    private Integer version;

    @Column(unique = true)
    @Pattern(
    regexp = "^[a-zA-Z0-9._-]+@[a-zA-Z0-9.-]+$",
    message = "Invalid UPI handle format"
    )
    private String upiHandle;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;



    
}