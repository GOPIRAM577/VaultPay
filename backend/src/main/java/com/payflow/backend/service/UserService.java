package com.payflow.backend.service;

import com.payflow.backend.entity.User;
import com.payflow.backend.entity.Wallet;
import com.payflow.backend.entity.WalletStatus;
import com.payflow.backend.repository.UserRepository;
import com.payflow.backend.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;

    @Transactional
    public void softDeleteUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        user.setIsActive(false);
        userRepository.save(user);

        Optional<Wallet> walletOpt = walletRepository.findByUserId(userId);
        walletOpt.ifPresent(wallet -> {
            wallet.setStatus(WalletStatus.FROZEN);
            walletRepository.save(wallet);
        });
    }
}
