package com.payflow.backend.entity;

public enum PaymentStatus {
    INITIATED,
    FRAUD_REVIEW,
    PROCESSING,
    COMPLETED,
    FAILED,
    REJECTED
}
