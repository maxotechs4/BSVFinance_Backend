package com.microfinance.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SetCapitalRequest {

    @NotNull(message = "Capital amount is required")
    @DecimalMin(value = "0", message = "Capital amount cannot be negative")
    private BigDecimal capitalAmount;
}
