package com.microfinance.dto.request;

import com.microfinance.entity.enums.PaymentMethod;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequest {

    @NotNull(message = "Member is required")
    private Long memberId;

    @NotNull(message = "Week number is required")
    @Min(value = 1, message = "Week number must be between 1 and 53")
    @Max(value = 53, message = "Week number must be between 1 and 53")
    private Integer weekNumber;

    @NotNull(message = "Payment year is required")
    @Min(value = 2000, message = "Payment year is invalid")
    private Integer paymentYear;

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
