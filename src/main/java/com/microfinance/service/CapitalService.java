package com.microfinance.service;

import com.microfinance.dto.request.CapitalTransactionRequest;
import com.microfinance.dto.request.SetCapitalRequest;
import com.microfinance.dto.response.CapitalAccountResponse;
import com.microfinance.dto.response.CapitalTransactionResponse;

import java.math.BigDecimal;
import java.util.List;

public interface CapitalService {

    CapitalAccountResponse getAccount();

    CapitalAccountResponse setCapitalAmount(SetCapitalRequest request);

    List<CapitalTransactionResponse> listTransactions();

    CapitalTransactionResponse createTransaction(CapitalTransactionRequest request);

    CapitalTransactionResponse updateTransaction(Long id, CapitalTransactionRequest request);

    void deleteTransaction(Long id);

    BigDecimal[] drawForNewLoan(BigDecimal loanAmount);

    void reverseForDeletedLoan(BigDecimal capitalPortion, BigDecimal collectionPortion);
}
