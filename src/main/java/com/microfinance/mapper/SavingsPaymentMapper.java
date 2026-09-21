package com.microfinance.mapper;

import com.microfinance.dto.response.SavingsPaymentResponse;
import com.microfinance.entity.SavingsPayment;
import org.springframework.stereotype.Component;

@Component
public class SavingsPaymentMapper {

    public SavingsPaymentResponse toResponse(SavingsPayment payment) {
        if (payment == null) {
            return null;
        }
        return SavingsPaymentResponse.builder()
                .id(payment.getId())
                .savingsMemberId(payment.getSavingsMember().getId())
                .receiptNumber(payment.getReceiptNumber())
                .installmentNumber(payment.getInstallmentNumber())
                .monthlyAmount(payment.getMonthlyAmount())
                .amountPaid(payment.getAmountPaid())
                .remainingAmount(payment.getRemainingAmount())
                .extraAmount(payment.getExtraAmount())
                .status(payment.getStatus())
                .paymentMethod(payment.getPaymentMethod())
                .upiTransactionId(payment.getUpiTransactionId())
                .paymentDate(payment.getPaymentDate())
                .remarks(payment.getRemarks())
                .build();
    }
}
