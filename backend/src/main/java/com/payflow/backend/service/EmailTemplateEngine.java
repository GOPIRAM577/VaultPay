package com.payflow.backend.service;

import com.payflow.backend.entity.Payment;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class EmailTemplateEngine {

    public String generatePaymentCompletedSenderHtml(Payment payment, String receiverName) {
        return "<html><body>" +
               "<h2>Payment Sent!</h2>" +
               "<p>Your payment of <strong>" + payment.getAmount() + "</strong> to " + receiverName + " was successfully completed.</p>" +
               "<p>Transaction ID: " + payment.getId() + "</p>" +
               "</body></html>";
    }

    public String generatePaymentCompletedReceiverHtml(Payment payment, String senderName) {
        return "<html><body>" +
               "<h2>Payment Received!</h2>" +
               "<p>You have received a payment of <strong>" + payment.getAmount() + "</strong> from " + senderName + ".</p>" +
               "<p>Transaction ID: " + payment.getId() + "</p>" +
               "</body></html>";
    }

    public String generatePaymentFailedHtml(Payment payment, String receiverName) {
        return "<html><body>" +
               "<h2>Payment Failed</h2>" +
               "<p>Unfortunately, your payment of <strong>" + payment.getAmount() + "</strong> to " + receiverName + " has failed.</p>" +
               "<p>Transaction ID: " + payment.getId() + "</p>" +
               "<p>Please check your balance or contact support.</p>" +
               "</body></html>";
    }
}
