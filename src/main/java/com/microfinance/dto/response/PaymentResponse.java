package com.microfinance.dto.response;

import com.microfinance.entity.enums.PaymentMethod;
import com.microfinance.entity.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentResponse {
    private Long id;
    private String receiptNumber;
    private Long memberId;
    private String memberCode;
    private String memberName;
    private Integer weekNumber;
    private Integer paymentYear;
    private BigDecimal weeklyAmount;
    private BigDecimal amountPaid;
    private BigDecimal remainingAmount;
    private BigDecimal extraAmount;
    private PaymentStatus status;
    private PaymentMethod paymentMethod;
    private String upiTransactionId;
    private LocalDate paymentDate;
    private String remarks;
    private LocalDateTime createdAt;
}
