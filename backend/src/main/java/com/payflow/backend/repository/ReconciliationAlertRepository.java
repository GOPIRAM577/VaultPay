package com.payflow.backend.repository;

import com.payflow.backend.entity.ReconciliationAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.UUID;
import java.time.LocalDateTime;

public interface ReconciliationAlertRepository extends JpaRepository<ReconciliationAlert, UUID> {
    long countByResolvedAtIsNull();
    
    @Query("SELECT MAX(r.detectedAt) FROM ReconciliationAlert r")
    LocalDateTime findLastRunAt();
}
