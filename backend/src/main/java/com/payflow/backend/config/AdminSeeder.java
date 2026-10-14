package com.payflow.backend.config;

import com.payflow.backend.entity.Role;
import com.payflow.backend.entity.User;
import com.payflow.backend.entity.Wallet;
import com.payflow.backend.entity.WalletStatus;
import com.payflow.backend.repository.UserRepository;
import com.payflow.backend.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdminSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        String adminEmail = "admin@payflow.com";
        Optional<User> adminOpt = userRepository.findByEmail(adminEmail);
        
        if (adminOpt.isEmpty()) {
            User admin = User.builder()
                    .name("System Admin")
                    .email(adminEmail)
                    .phone("+10000000000") // Dummy phone
                    .passwordHash(passwordEncoder.encode("admin123"))
                    .role(Role.ADMIN)
                    .isActive(true)
                    .build();
            User savedAdmin = userRepository.save(admin);
            
            Wallet adminWallet = Wallet.builder()
                    .userId(savedAdmin.getId())
                    .balance(new BigDecimal("0.00"))
                    .currency("INR")
                    .status(WalletStatus.ACTIVE)
                    .upiHandle("admin@payflow")
                    .build();
            walletRepository.save(adminWallet);
            
            log.info("Seeded default admin user: {} with password: {}", adminEmail, "admin123");
        }
    }
}
