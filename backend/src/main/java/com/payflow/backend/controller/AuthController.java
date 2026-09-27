package com.payflow.backend.controller;

import com.payflow.backend.dto.RegisterRequest;
import com.payflow.backend.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import com.payflow.backend.dto.AuthResponse;
import com.payflow.backend.dto.LoginRequest;

import org.springframework.web.bind.annotation.*;
import com.payflow.backend.dto.LoginRequest;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request) {

        String token = authService.register(request);

        return ResponseEntity.ok(
                new AuthResponse(token, "Bearer")
        );
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        String token = authService.login(request);
        return ResponseEntity.ok(
                new AuthResponse(token, "Bearer"));
}
}