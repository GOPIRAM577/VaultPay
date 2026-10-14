package com.payflow.backend.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.UUID;

@Data
public class PaymentRequest {
    private UUID receiverWalletId;
    private BigDecimal amount;
}
