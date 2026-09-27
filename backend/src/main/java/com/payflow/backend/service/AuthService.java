package com.payflow.backend.service;

import com.payflow.backend.dto.RegisterRequest;
import com.payflow.backend.entity.Role;
import com.payflow.backend.entity.User;
import com.payflow.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.payflow.backend.dto.LoginRequest;
import org.springframework.data.redis.core.StringRedisTemplate;
import java.util.concurrent.TimeUnit;
import com.payflow.backend.exception.AccountLockedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final StringRedisTemplate redisTemplate;
    private final EmailService emailService;
    private final WalletService walletService;
    
    @Transactional
    public String register(RegisterRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalStateException("Email already exists");
        }

        if (userRepository.existsByPhone(request.getPhone())) {
            throw new IllegalStateException("Phone already exists");
        }

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .isActive(true)
                .build();

        userRepository.save(user);
        walletService.createWallet(user.getId());

        try {
            emailService.sendWelcomeEmail(
                    user.getEmail(),
                    user.getName());
        } catch (Exception e) {
            log.error("Failed to send welcome email", e);
        }

        return jwtService.generateToken(
                user.getId(),
                user.getEmail(),
                user.getRole().name());
    }

    private void incrementFailureCounter(String key) {

        Long attempts = redisTemplate.opsForValue().increment(key);

        if (attempts != null && attempts == 1) {
            redisTemplate.expire(key, 10, TimeUnit.MINUTES);
        }
    }

    public String login(LoginRequest request) {

        // System.out.println(redisTemplate.hasKey("test"));
        String key = "login:fail:" + request.getEmail();
        String count = redisTemplate.opsForValue().get(key);

        if (count != null && Integer.parseInt(count) >= 5) {
            throw new AccountLockedException(
                    "Account locked. Try again after 10 minutes.");
        }
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    incrementFailureCounter(key);
                    return new IllegalArgumentException("Invalid email or password");
                });

        if (!user.getIsActive()) {
            throw new AccountLockedException("Your account has been deactivated. Please contact support.");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            incrementFailureCounter(key);
            throw new IllegalArgumentException("Invalid email or password");
        }

        redisTemplate.delete(key);

        return jwtService.generateToken(
                user.getId(),
                user.getEmail(),
                user.getRole().name());
    }
}