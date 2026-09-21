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
public class SavingsPaymentRequest {

    @NotNull(message = "Savings member is required")
    private Long savingsMemberId;

    @NotNull(message = "Installment number is required")
    @Min(value = 1, message = "Installment number must be at least 1")
    @Max(value = 36, message = "Installment number cannot exceed 36")
    private Integer installmentNumber;

    @NotNull(message = "Amount paid is required")
    @DecimalMin(value = "0.01", message = "Amount paid must be greater than zero")
    private BigDecimal amountPaid;

    @NotNull(message = "Payment date is required")
    private LocalDate paymentDate;

    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;

    private String upiTransactionId;

    @Size(max = 500, message = "Remarks must not exceed 500 characters")
    private String remarks;
}
