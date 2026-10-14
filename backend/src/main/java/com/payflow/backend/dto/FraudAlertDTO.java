package com.payflow.backend.dto;

import com.payflow.backend.entity.FraudAssessment;
import com.payflow.backend.entity.Payment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FraudAlertDTO {
    private Payment payment;
    private FraudAssessment fraudAssessment;
}
