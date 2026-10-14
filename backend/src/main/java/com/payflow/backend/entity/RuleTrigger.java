package com.payflow.backend.entity;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RuleTrigger {
    private String ruleName;
    private double scoreContribution;
    private boolean triggered;
}
