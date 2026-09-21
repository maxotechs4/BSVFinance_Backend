package com.microfinance.dto.response;

import com.microfinance.entity.enums.CapitalTransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CapitalTransactionResponse {
    private Long id;
    private CapitalTransactionType type;
    private BigDecimal amount;
    private String purpose;
    private LocalDate transactionDate;
}
