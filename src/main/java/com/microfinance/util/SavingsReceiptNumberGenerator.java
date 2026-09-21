package com.microfinance.util;

import com.microfinance.repository.SavingsPaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Year;

@Component
@RequiredArgsConstructor
public class SavingsReceiptNumberGenerator {

    private final SavingsPaymentRepository savingsPaymentRepository;

    /** Generates receipt numbers like SRCPT-2026-000123. */
    public String generate(int attempt) {
        long nextSeq = savingsPaymentRepository.count() + 1 + attempt;
        int year = Year.now().getValue();
        return String.format("SRCPT-%d-%06d", year, nextSeq);
    }
}
