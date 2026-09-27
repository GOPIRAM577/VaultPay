package com.payflow.backend.service;

import com.payflow.backend.entity.Payment;
import com.payflow.backend.entity.PaymentStatus;
import com.payflow.backend.exception.IllegalStateTransitionException;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

@Component
public class PaymentStateMachine {

    private final Map<PaymentStatus, Set<PaymentStatus>> validTransitions;

    public PaymentStateMachine() {
        validTransitions = new EnumMap<>(PaymentStatus.class);
        
        validTransitions.put(PaymentStatus.INITIATED, EnumSet.of(PaymentStatus.FRAUD_REVIEW, PaymentStatus.FAILED));
        validTransitions.put(PaymentStatus.FRAUD_REVIEW, EnumSet.of(PaymentStatus.PROCESSING, PaymentStatus.REJECTED));
        validTransitions.put(PaymentStatus.PROCESSING, EnumSet.of(PaymentStatus.COMPLETED, PaymentStatus.FAILED));
        validTransitions.put(PaymentStatus.COMPLETED, EnumSet.noneOf(PaymentStatus.class));
        validTransitions.put(PaymentStatus.FAILED, EnumSet.noneOf(PaymentStatus.class));
        validTransitions.put(PaymentStatus.REJECTED, EnumSet.noneOf(PaymentStatus.class));
    }

    public void transition(Payment payment, PaymentStatus target) {
        PaymentStatus current = payment.getStatus();
        
        Set<PaymentStatus> allowed = validTransitions.getOrDefault(current, EnumSet.noneOf(PaymentStatus.class));
        
        if (!allowed.contains(target)) {
            throw new IllegalStateTransitionException("Cannot transition payment from " + current + " to " + target);
        }
        
        payment.setStatus(target);
    }
}