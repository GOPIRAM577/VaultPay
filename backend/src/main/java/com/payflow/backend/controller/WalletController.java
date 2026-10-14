package com.payflow.backend.controller;

import com.payflow.backend.dto.BalanceResponseDTO;
import com.payflow.backend.dto.TopupRequest;
import com.payflow.backend.dto.WalletDTO;
import com.payflow.backend.dto.TransactionResponseDTO;
import com.payflow.backend.service.LedgerService;
import com.payflow.backend.service.WalletCacheService;
import com.payflow.backend.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/wallets")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;
    private final WalletCacheService walletCacheService;
    private final LedgerService ledgerService;

    @PostMapping("/topup")
    public ResponseEntity<WalletDTO> topup(Authentication authentication, @RequestBody TopupRequest topupRequest) {
        UUID userId = UUID.fromString((String) authentication.getPrincipal());
        if (topupRequest.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return ResponseEntity.badRequest().build();
        }
        WalletDTO wallet = walletService.topup(userId, topupRequest.getAmount());
        return ResponseEntity.ok(wallet);
    }

    @GetMapping("/me")
    public ResponseEntity<WalletDTO> me(Authentication authentication) {
        UUID userId = UUID.fromString((String) authentication.getPrincipal());
        return walletService.getWalletByUserId(userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/me/balance")
    public ResponseEntity<BalanceResponseDTO> getBalance(Authentication authentication) {
        UUID userId = UUID.fromString((String) authentication.getPrincipal());

        Optional<WalletDTO> walletOpt = walletService.getWalletByUserId(userId);
        if (walletOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        WalletDTO wallet = walletOpt.get();
        UUID walletId = wallet.getId();

        Optional<BigDecimal> cachedBalance = walletCacheService.getBalanceFromCache(walletId);
        if (cachedBalance.isPresent()) {
            return ResponseEntity.ok(new BalanceResponseDTO(cachedBalance.get(), wallet.getCurrency()));
        }

        BigDecimal balance = wallet.getBalance();
        walletCacheService.saveBalanceToCache(walletId, balance);

        return ResponseEntity.ok(new BalanceResponseDTO(balance, wallet.getCurrency()));
    }

    @GetMapping(value = "/me/qr", produces = "image/png")
    public ResponseEntity<byte[]> getMyQrCode(Authentication authentication) {
        UUID userId = UUID.fromString((String) authentication.getPrincipal());
        Optional<WalletDTO> walletOpt = walletService.getWalletByUserId(userId);
        
        if (walletOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        String url = "https://payflow.vercel.app/pay?to=" + walletOpt.get().getId();
        
        try {
            com.google.zxing.qrcode.QRCodeWriter qrCodeWriter = new com.google.zxing.qrcode.QRCodeWriter();
            com.google.zxing.common.BitMatrix bitMatrix = qrCodeWriter.encode(
                    url, com.google.zxing.BarcodeFormat.QR_CODE, 300, 300);
                    
            java.io.ByteArrayOutputStream pngOutputStream = new java.io.ByteArrayOutputStream();
            com.google.zxing.client.j2se.MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
            
            byte[] pngData = pngOutputStream.toByteArray();
            
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.IMAGE_PNG);
            
            return new ResponseEntity<>(pngData, headers, org.springframework.http.HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/me/transactions")
    public ResponseEntity<TransactionResponseDTO> getTransactions(
            Authentication authentication,
            @RequestParam(required = false, defaultValue = "0") Long cursor,
            @RequestParam(required = false, defaultValue = "20") int limit) {
        
        UUID userId = UUID.fromString((String) authentication.getPrincipal());
        Optional<WalletDTO> walletOpt = walletService.getWalletByUserId(userId);
        
        if (walletOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        TransactionResponseDTO response = ledgerService.getTransactions(walletOpt.get().getId(), cursor, limit);
        return ResponseEntity.ok(response);
    }
}