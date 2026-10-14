package com.payflow.backend.service;

import com.payflow.backend.entity.Payment;
import com.payflow.backend.entity.PaymentStatus;
import com.payflow.backend.exception.IllegalStateTransitionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentStateMachineTest {

    private PaymentStateMachine stateMachine;

    @BeforeEach
    void setUp() {
        stateMachine = new PaymentStateMachine();
    }

    @Test
    void testValidTransitions() {
        Payment payment = new Payment();
        
        // INITIATED -> FRAUD_REVIEW
        payment.setStatus(PaymentStatus.INITIATED);
        stateMachine.transition(payment, PaymentStatus.FRAUD_REVIEW);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FRAUD_REVIEW);

        // FRAUD_REVIEW -> PROCESSING
        stateMachine.transition(payment, PaymentStatus.PROCESSING);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PROCESSING);

        // PROCESSING -> COMPLETED
        stateMachine.transition(payment, PaymentStatus.COMPLETED);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.COMPLETED);

        // INITIATED -> FAILED
        payment.setStatus(PaymentStatus.INITIATED);
        stateMachine.transition(payment, PaymentStatus.FAILED);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);

        // FRAUD_REVIEW -> REJECTED
        payment.setStatus(PaymentStatus.FRAUD_REVIEW);
        stateMachine.transition(payment, PaymentStatus.REJECTED);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REJECTED);
        
        // PROCESSING -> FAILED
        payment.setStatus(PaymentStatus.PROCESSING);
        stateMachine.transition(payment, PaymentStatus.FAILED);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
    }

    @Test
    void testInvalidTransitions() {
        Payment payment = new Payment();
        
        // INITIATED -> COMPLETED (skip review)
        payment.setStatus(PaymentStatus.INITIATED);
        assertThatThrownBy(() -> stateMachine.transition(payment, PaymentStatus.COMPLETED))
                .isInstanceOf(IllegalStateTransitionException.class);
                
        // INITIATED -> PROCESSING
        assertThatThrownBy(() -> stateMachine.transition(payment, PaymentStatus.PROCESSING))
                .isInstanceOf(IllegalStateTransitionException.class);
                
        // COMPLETED -> PROCESSING (backwards)
        payment.setStatus(PaymentStatus.COMPLETED);
        assertThatThrownBy(() -> stateMachine.transition(payment, PaymentStatus.PROCESSING))
                .isInstanceOf(IllegalStateTransitionException.class);

        // FAILED -> INITIATED
        payment.setStatus(PaymentStatus.FAILED);
        assertThatThrownBy(() -> stateMachine.transition(payment, PaymentStatus.INITIATED))
                .isInstanceOf(IllegalStateTransitionException.class);

        // FRAUD_REVIEW -> COMPLETED
        payment.setStatus(PaymentStatus.FRAUD_REVIEW);
        assertThatThrownBy(() -> stateMachine.transition(payment, PaymentStatus.COMPLETED))
                .isInstanceOf(IllegalStateTransitionException.class);
    }
}
