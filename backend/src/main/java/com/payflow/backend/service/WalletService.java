package com.payflow.backend.service;

import com.payflow.backend.dto.WalletDTO;
import com.payflow.backend.entity.LedgerEntryType;
import com.payflow.backend.entity.Wallet;
import com.payflow.backend.entity.WalletStatus;
import com.payflow.backend.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class WalletService {

    private final WalletRepository walletRepository;
    private final LedgerService ledgerService;
    private final WalletCacheService walletCacheService;

    @Transactional
    public WalletDTO topup(UUID userId, BigDecimal amount) {
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Wallet not found"));
        
        wallet.setBalance(wallet.getBalance().add(amount));
        Wallet savedWallet = walletRepository.save(wallet);
        
        ledgerService.writeSingleEntry(null, savedWallet.getId(), LedgerEntryType.CREDIT, amount, "Topup");
        walletCacheService.invalidateBalanceCache(savedWallet.getId());
        
        log.info("Topup of {} to wallet {}", amount, savedWallet.getId());
        return mapToDTO(savedWallet);
    }

    public Wallet createWallet(UUID userId) {


        Wallet wallet = Wallet.builder()
                .userId(userId)
                .balance(new BigDecimal("1000.00"))
                .currency("INR")
                .status(WalletStatus.ACTIVE)
                .build();

        Wallet savedWallet = walletRepository.save(wallet);

        log.info("Wallet created for user {}", userId);

        return savedWallet;
    }

    public Optional<WalletDTO> getWalletByUserId(UUID userId) {
        return walletRepository.findByUserId(userId)
                .map(this::mapToDTO);
    }

    private WalletDTO mapToDTO(Wallet wallet) {
        return WalletDTO.builder()
                .id(wallet.getId())
                .userId(wallet.getUserId())
                .balance(wallet.getBalance())
                .currency(wallet.getCurrency())
                .status(wallet.getStatus())
                .upiHandle(wallet.getUpiHandle())
                .createdAt(wallet.getCreatedAt())
                .build();
    }
}