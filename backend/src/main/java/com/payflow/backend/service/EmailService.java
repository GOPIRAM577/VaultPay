package com.payflow.backend.service;

import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EmailService {

    private final Resend resend;

    public EmailService(@Value("${resend.api-key}") String apiKey) {
        this.resend = new Resend(apiKey);
    }

    public void sendWelcomeEmail(String to, String name) {

        CreateEmailOptions params = CreateEmailOptions.builder()
                .from("Payflow <onboarding@resend.dev>")
                .to(to)
                .subject("Welcome to PayFlow!")
                .html("""
                        <h2>Welcome to PayFlow, %s!</h2>
                        <p>Your account has been created successfully.</p>
                        <p>We're excited to have you on board.</p>
                        """.formatted(name))
                .build();

        try {
            resend.emails().send(params);
        } catch (ResendException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        log.info("Welcome email sent to {}", to);
    }
}