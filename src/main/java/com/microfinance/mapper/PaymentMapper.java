package com.microfinance.mapper;

import com.microfinance.dto.response.PaymentResponse;
import com.microfinance.entity.Payment;
import org.springframework.stereotype.Component;

@Component
public class PaymentMapper {

    public PaymentResponse toResponse(Payment payment) {
        if (payment == null) {
            return null;
        }
        return PaymentResponse.builder()
                .id(payment.getId())
                .receiptNumber(payment.getReceiptNumber())
                .memberId(payment.getMember().getId())
                .memberCode(payment.getMember().getMemberCode())
                .memberName(payment.getMember().getName())
                .weekNumber(payment.getWeekNumber())
                .paymentYear(payment.getPaymentYear())
                .weeklyAmount(payment.getWeeklyAmount())
                .amountPaid(payment.getAmountPaid())
                .remainingAmount(payment.getRemainingAmount())
                .extraAmount(payment.getExtraAmount())
                .status(payment.getStatus())
                .paymentMethod(payment.getPaymentMethod())
                .upiTransactionId(payment.getUpiTransactionId())
                .paymentDate(payment.getPaymentDate())
                .remarks(payment.getRemarks())
                .createdAt(payment.getCreatedAt())
                .build();
    }
}
