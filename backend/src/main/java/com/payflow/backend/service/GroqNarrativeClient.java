package com.payflow.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payflow.backend.entity.FraudAssessment;
import com.payflow.backend.entity.Payment;
import com.payflow.backend.repository.FraudAssessmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class GroqNarrativeClient {

    private final FraudAssessmentRepository fraudAssessmentRepository;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${groq.api-key}")
    private String groqApiKey;

    @Async
    public void generateAndStoreNarrative(java.math.BigDecimal amount, UUID assessmentId) {
        try {
            // Mocking these values for now as per plan
            String velocityCount = "3 payments in last 24h";
            String walletAgeHours = "48";
            String recipientRelationship = "new_recipient";
            String timeOfDay = "14:30 IST";
            
            String prompt = String.format(
                "Write a 2-sentence risk assessment for this payment: Amount: %s, Velocity: %s, Wallet Age: %s hrs, Relationship: %s, Time: %s.",
                amount, velocityCount, walletAgeHours, recipientRelationship, timeOfDay
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(groqApiKey);

            Map<String, Object> requestBody = Map.of(
                "model", "openai/gpt-oss-20b", // Standard fast model on Groq
                "messages", List.of(
                    Map.of("role", "user", "content", prompt)
                )
            );

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(
                "https://api.groq.com/openai/v1/chat/completions",
                entity,
                String.class
            );

            String content = "AI narrative unavailable";
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> responseMap = objectMapper.readValue(response.getBody(), Map.class);
                List<Map<String, Object>> choices = (List<Map<String, Object>>) responseMap.get("choices");
                if (choices != null && !choices.isEmpty()) {
                    Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                    String aiText = (String) message.get("content");
                    content = aiText.trim();
                }
            }

            FraudAssessment assessment = fraudAssessmentRepository.findById(assessmentId)
                    .orElseThrow(() -> new RuntimeException("Assessment not found"));
            assessment.setAiNarrative(content);
            fraudAssessmentRepository.save(assessment);

        } catch (Exception e) {
            log.error("Failed to generate AI narrative", e);
            try {
                FraudAssessment assessment = fraudAssessmentRepository.findById(assessmentId).orElse(null);
                if (assessment != null) {
                    assessment.setAiNarrative("AI narrative unavailable");
                    fraudAssessmentRepository.save(assessment);
                }
            } catch (Exception ex) {
                log.error("Failed to save fallback AI narrative", ex);
            }
        }
    }
}
