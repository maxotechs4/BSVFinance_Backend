package com.microfinance.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CapitalAccountResponse {

    /** Current capital balance available to fund new loans. */
    private BigDecimal capitalAmount;

    /**
     * Cumulative amount that has been drawn from "Total Collection" to
     * cover loans that exceeded the available capital.
     */
    private BigDecimal collectionUtilized;

    /** Total collected from members across all time (all payments ever made). */
    private BigDecimal totalCollectionAllTime;

    /**
     * capitalAmount + totalCollectionAllTime - collectionUtilized: the
     * combined pool the admin actually has to work with, after accounting
     * for anything drawn out by loan overflow or Expense entries (and
     * anything added back by Income entries).
     */
    private BigDecimal totalAmount;
}
