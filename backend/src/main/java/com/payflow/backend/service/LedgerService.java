package com.payflow.backend.service;

import com.payflow.backend.entity.LedgerEntry;
import com.payflow.backend.entity.LedgerEntryType;
import com.payflow.backend.repository.LedgerEntryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.payflow.backend.dto.TransactionResponseDTO;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class LedgerService {

    private final LedgerEntryRepository ledgerEntryRepository;

    @Transactional
    public void writeDoubleEntry(UUID paymentId, UUID senderWalletId, UUID receiverWalletId, BigDecimal amount) {
        
        LedgerEntry debitEntry = LedgerEntry.builder()
                .paymentId(paymentId)
                .walletId(senderWalletId)
                .entryType(LedgerEntryType.DEBIT)
                .amount(amount)
                .description("Payment sent")
                .build();

        LedgerEntry creditEntry = LedgerEntry.builder()
                .paymentId(paymentId)
                .walletId(receiverWalletId)
                .entryType(LedgerEntryType.CREDIT)
                .amount(amount)
                .description("Payment received")
                .build();

        ledgerEntryRepository.save(debitEntry);
        ledgerEntryRepository.save(creditEntry);

        log.info("Double entry written for payment {} (Sender: {}, Receiver: {})", paymentId, senderWalletId, receiverWalletId);
    }

    @Transactional
    public void writeSingleEntry(UUID paymentId, UUID walletId, LedgerEntryType type, BigDecimal amount, String description) {
        LedgerEntry entry = LedgerEntry.builder()
                .paymentId(paymentId)
                .walletId(walletId)
                .entryType(type)
                .amount(amount)
                .description(description)
                .build();
        
        ledgerEntryRepository.save(entry);
        log.info("Single {} entry written for wallet {} with amount {}", type, walletId, amount);
    }

    public TransactionResponseDTO getTransactions(UUID walletId, Long cursor, int limit) {
        Pageable pageable = PageRequest.of(0, limit + 1);
        List<LedgerEntry> entries = ledgerEntryRepository.findTransactionsByCursor(walletId, cursor, pageable);

        boolean hasMore = entries.size() > limit;
        if (hasMore) {
            entries.remove(entries.size() - 1);
        }

        Long nextCursor = entries.isEmpty() ? null : entries.get(entries.size() - 1).getSeq();

        return TransactionResponseDTO.builder()
                .transactions(entries)
                .nextCursor(nextCursor)
                .hasMore(hasMore)
                .build();
    }
}
