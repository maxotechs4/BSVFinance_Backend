package com.microfinance.dto.request;

import com.microfinance.entity.enums.CapitalTransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CapitalTransactionRequest {

    @NotNull(message = "Type is required (INCOME or EXPENSE)")
    private CapitalTransactionType type;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;

    private String purpose;

    /** Defaults to today on the server if not supplied. */
    private LocalDate transactionDate;
}
