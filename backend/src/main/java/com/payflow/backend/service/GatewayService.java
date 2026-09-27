package com.payflow.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class GatewayService {

    private final double failureRate;

    public GatewayService(@Value("${GATEWAY_FAILURE_RATE:0.1}") double failureRate) {
        this.failureRate = failureRate;
    }

    public GatewayResponse simulateCapture() {
        double rand = Math.random();
        if (rand >= failureRate) {
            log.info("Gateway capture successful");
            return GatewayResponse.GATEWAY_SUCCESS;
        } else {
            log.warn("Gateway capture declined: INSUFFICIENT_GATEWAY_FUNDS");
            return GatewayResponse.GATEWAY_DECLINED;
        }
    }
}
