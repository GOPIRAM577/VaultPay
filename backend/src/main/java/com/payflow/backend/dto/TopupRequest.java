package com.payflow.backend.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class TopupRequest {
    private BigDecimal amount;
}
