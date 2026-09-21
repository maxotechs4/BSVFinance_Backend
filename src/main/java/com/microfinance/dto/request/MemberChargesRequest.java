package com.microfinance.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Admin-only request to set a member's one-time insurance and processing
 * charges. Deliberately separate from MemberRequest since this is only
 * ever submitted from the Admin page, by an Admin, never through the
 * regular create/edit member form.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MemberChargesRequest {

    @NotNull(message = "Insurance amount is required")
    @DecimalMin(value = "0", message = "Insurance amount cannot be negative")
    private BigDecimal insuranceAmount;

    @NotNull(message = "Processing amount is required")
    @DecimalMin(value = "0", message = "Processing amount cannot be negative")
    private BigDecimal processingAmount;
}
