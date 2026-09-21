package com.microfinance.dto.request;

import com.microfinance.entity.enums.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Used by the Collection page. Unlike {@link PaymentRequest}, the caller does NOT supply
 * weekNumber/paymentYear - the server works out the member's next week from their existing
 * payment history (last recorded week + 1), and updates that week's entry instead of creating
 * a duplicate if it already exists.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CollectionRequest {

    @NotNull(message = "Member is required")
    private Long memberId;

    @NotNull(message = "Amount paid is required")
    @DecimalMin(value = "0.01", message = "Amount paid cannot be negative or zero")
    @Digits(integer = 10, fraction = 2, message = "Amount paid has an invalid format")
    private BigDecimal amountPaid;

    @NotNull(message = "Payment date is required")
    private LocalDate paymentDate;

    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;

    /** Required only when paymentMethod = ONLINE; enforced in the service layer. */
    private String upiTransactionId;

    @Size(max = 500, message = "Remarks must not exceed 500 characters")
    private String remarks;
}