package com.payflow.backend.dto;

import com.payflow.backend.entity.LedgerEntry;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionResponseDTO {
    private List<LedgerEntry> transactions;
    private Long nextCursor;
    private boolean hasMore;
}
