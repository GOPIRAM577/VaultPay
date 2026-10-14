package com.payflow.backend.service;

import com.payflow.backend.entity.LedgerEntry;
import com.payflow.backend.entity.LedgerEntryType;
import com.payflow.backend.entity.SettlementReport;
import com.payflow.backend.entity.Wallet;
import com.payflow.backend.repository.LedgerEntryRepository;
import com.payflow.backend.repository.SettlementReportRepository;
import com.payflow.backend.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SettlementService {

    private final WalletRepository walletRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final SettlementReportRepository settlementReportRepository;

    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Kolkata")
    public void generateSettlementReports() {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.plusDays(1).atStartOfDay();

        log.info("Starting settlement report generation for {}", today);

        List<Wallet> wallets = walletRepository.findAll();
        for (Wallet wallet : wallets) {
            List<LedgerEntry> entries = ledgerEntryRepository.findAllByWalletIdOrderByCreatedAtAsc(wallet.getId());
            
            BigDecimal totalCredits = BigDecimal.ZERO;
            BigDecimal totalDebits = BigDecimal.ZERO;

            for (LedgerEntry entry : entries) {
                if (entry.getCreatedAt().isAfter(startOfDay) && entry.getCreatedAt().isBefore(endOfDay)) {
                    if (entry.getEntryType() == LedgerEntryType.CREDIT) {
                        totalCredits = totalCredits.add(entry.getAmount());
                    } else if (entry.getEntryType() == LedgerEntryType.DEBIT) {
                        totalDebits = totalDebits.add(entry.getAmount());
                    }
                }
            }

            BigDecimal netPosition = totalCredits.subtract(totalDebits);

            SettlementReport report = SettlementReport.builder()
                    .reportDate(today)
                    .walletId(wallet.getId())
                    .totalCredits(totalCredits)
                    .totalDebits(totalDebits)
                    .netPosition(netPosition)
                    .build();

            settlementReportRepository.save(report);
        }

        log.info("Finished settlement report generation for {} wallets", wallets.size());
    }
}
