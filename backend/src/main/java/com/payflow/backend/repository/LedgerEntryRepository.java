package com.payflow.backend.repository;

import com.payflow.backend.entity.LedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, UUID> {
    
    List<LedgerEntry> findByPaymentId(UUID paymentId);
    
    List<LedgerEntry> findByWalletId(UUID walletId);
    
    List<LedgerEntry> findAllByWalletIdOrderByCreatedAtAsc(UUID walletId);
    
    @Query("SELECT l FROM LedgerEntry l WHERE l.walletId = :walletId AND l.seq > :cursor ORDER BY l.seq ASC")
    List<LedgerEntry> findTransactionsByCursor(@Param("walletId") UUID walletId, @Param("cursor") Long cursor, Pageable pageable);
    
    @Query("SELECT COALESCE(SUM(l.amount), 0) FROM LedgerEntry l WHERE l.walletId = :walletId")
    BigDecimal sumAmountsByWalletId(@Param("walletId") UUID walletId);
}
