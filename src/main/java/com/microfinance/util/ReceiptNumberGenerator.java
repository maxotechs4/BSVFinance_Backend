package com.microfinance.util;

import com.microfinance.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Year;

@Component
@RequiredArgsConstructor
public class ReceiptNumberGenerator {

    private final PaymentRepository paymentRepository;

    /**
     * Generates receipt numbers like RCPT-2026-000123.
     *
     * IMPORTANT: this is based on the highest receipt number ever issued for
     * the year, NOT paymentRepository.count(). A row count goes backwards
     * every time any payment is deleted (including via cascade-delete when a
     * member is removed), which caused the next generated number to collide
     * with one that's still on record — a real bug this replaces.
     */
    public String generate(int attempt) {
        int year = Year.now().getValue();
        String prefix = "RCPT-" + year + "-";

        long lastSeq = paymentRepository.findTopByReceiptNumberStartingWithOrderByReceiptNumberDesc(prefix)
                .map(p -> {
                    String suffix = p.getReceiptNumber().substring(prefix.length());
                    try {
                        return Long.parseLong(suffix);
                    } catch (NumberFormatException e) {
                        return 0L;
                    }
                })
                .orElse(0L);

        long nextSeq = lastSeq + 1 + attempt;
        return String.format("%s%06d", prefix, nextSeq);
    }
}