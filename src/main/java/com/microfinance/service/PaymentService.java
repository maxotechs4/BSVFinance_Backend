package com.microfinance.service;

import com.microfinance.dto.request.CollectionRequest;
import com.microfinance.dto.request.PaymentRequest;
import com.microfinance.dto.response.PageResponse;
import com.microfinance.dto.response.PaymentResponse;
import com.microfinance.entity.enums.PaymentMethod;

import java.time.LocalDate;
import java.util.List;

public interface PaymentService {

    PaymentResponse addPayment(PaymentRequest request);

    /**
     * Collection-page entry point: saves the given amount against the member's next
     * week (their last recorded week + 1), updating that week's entry instead of
     * creating a duplicate if it was already recorded.
     */
    PaymentResponse collectNextWeekPayment(CollectionRequest request);

    PaymentResponse updatePayment(Long id, PaymentRequest request);

    void deletePayment(Long id);

    PaymentResponse getPaymentById(Long id);

    List<PaymentResponse> getPaymentsByMember(Long memberId);

    PageResponse<PaymentResponse> getPayments(Long memberId, Integer weekNumber, Integer paymentYear,
                                               PaymentMethod method, LocalDate startDate, LocalDate endDate,
                                               int page, int size);
}