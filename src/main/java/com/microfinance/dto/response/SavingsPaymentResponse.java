package com.microfinance.dto.response;

import com.microfinance.entity.enums.PaymentMethod;
import com.microfinance.entity.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SavingsPaymentResponse {

    private Long id;
    private Long savingsMemberId;
    private String receiptNumber;
    private Integer installmentNumber;
    private BigDecimal monthlyAmount;
    private BigDecimal amountPaid;
    private BigDecimal remainingAmount;
    private BigDecimal extraAmount;
    private PaymentStatus status;
    private PaymentMethod paymentMethod;
    private String upiTransactionId;
    private LocalDate paymentDate;
    private String remarks;
}
