package com.payflow.backend.service;

import com.payflow.backend.entity.ReconciliationAlert;
import com.payflow.backend.entity.Wallet;
import com.payflow.backend.repository.LedgerEntryRepository;
import com.payflow.backend.repository.ReconciliationAlertRepository;
import com.payflow.backend.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReconciliationService {

    private final WalletRepository walletRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final ReconciliationAlertRepository reconciliationAlertRepository;

    @Scheduled(fixedDelay = 3600000)
    public void runReconciliation() {
        log.info("Starting scheduled reconciliation job");
        List<Wallet> wallets = walletRepository.findAll();

        int mismatchCount = 0;

        for (Wallet wallet : wallets) {
            BigDecimal ledgerSum = ledgerEntryRepository.sumAmountsByWalletId(wallet.getId());
            BigDecimal expectedBalance = ledgerSum != null ? ledgerSum : BigDecimal.ZERO;
            BigDecimal actualBalance = wallet.getBalance() != null ? wallet.getBalance() : BigDecimal.ZERO;

            if (expectedBalance.compareTo(actualBalance) != 0) {
                mismatchCount++;
                BigDecimal discrepancy = expectedBalance.subtract(actualBalance);
                
                log.warn("Reconciliation mismatch detected for wallet {}. Expected: {}, Actual: {}", 
                        wallet.getId(), expectedBalance, actualBalance);

                ReconciliationAlert alert = ReconciliationAlert.builder()
                        .walletId(wallet.getId())
                        .expectedBalance(expectedBalance)
                        .actualBalance(actualBalance)
                        .discrepancy(discrepancy)
                        .build();

                reconciliationAlertRepository.save(alert);
            }
        }

        log.info("Finished scheduled reconciliation job. Mismatches detected: {}", mismatchCount);
    }
}
