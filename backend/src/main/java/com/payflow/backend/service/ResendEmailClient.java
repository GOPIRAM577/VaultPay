package com.payflow.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResendEmailClient {

    @Value("${RESEND_API_KEY}")
    private String resendApiKey;

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public void sendEmail(String to, String subject, String html) {
        log.info("[MOCK EMAIL] Bypassing Resend API. Email simulated successfully to: {} | Subject: '{}'", to, subject);
    }
}
