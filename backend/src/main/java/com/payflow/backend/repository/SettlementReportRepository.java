package com.payflow.backend.repository;

import com.payflow.backend.entity.SettlementReport;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface SettlementReportRepository extends JpaRepository<SettlementReport, UUID> {
    List<SettlementReport> findByReportDate(LocalDate reportDate);
}
