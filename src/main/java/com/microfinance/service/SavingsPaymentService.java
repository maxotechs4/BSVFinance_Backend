package com.microfinance.service;

import com.microfinance.dto.request.SavingsPaymentRequest;
import com.microfinance.dto.response.SavingsPaymentResponse;

import java.util.List;

public interface SavingsPaymentService {

    SavingsPaymentResponse addPayment(SavingsPaymentRequest request);

    SavingsPaymentResponse updatePayment(Long id, SavingsPaymentRequest request);

    void deletePayment(Long id);

    List<SavingsPaymentResponse> getPaymentsBySavingsMember(Long savingsMemberId);
}
