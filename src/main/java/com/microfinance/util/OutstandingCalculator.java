package com.microfinance.util;

import com.microfinance.entity.Payment;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Splits each weekly/monthly collection into a fixed Interest portion and a
 * Principal portion, then derives the current Outstanding Amount as the
 * total planned collection for the loan minus the principal collected so
 * far.
 *
 * Interest per payment (FIXED — based on the loan amount, not the amount
 * actually paid that week):
 *
 *   interest = ((loanAmount * interestPercentage) / 100) / 12
 *
 * Example: ₹15,000 loan at 18% interest
 *   -> interest = ((15,000 * 18) / 100) / 12 = ₹225 per payment
 *   -> a ₹700 weekly payment splits into ₹225 interest + ₹475 principal
 *
 * Total amount to be collected for the loan = weeklyAmount * totalWeeks
 * (e.g. ₹700 × 26 weeks = ₹18,200 for the ₹15,000 plan) — this, not the raw
 * loan amount, is what Outstanding Amount counts down from:
 *
 *   outstanding = (weeklyAmount * totalWeeks) - (principal collected so far)
 *
 * If totalWeeks isn't known for a member (e.g. a custom/legacy loan not tied
 * to one of the preset plans), this falls back to using loanAmount as the
 * total-to-collect figure, matching the pre-existing behaviour.
 *
 * Recomputing from the full payment history (rather than mutating a running
 * total) means the figure stays correct even if a payment is later edited or
 * deleted, or if the interest percentage itself is changed after the fact.
 */
public final class OutstandingCalculator {

    private OutstandingCalculator() {
    }

    /**
     * Splits a single payment amount into [interest, principal].
     *
     * Interest is FIXED per payment — it depends on the loan amount and the
     * interest rate, not on how much was actually paid that week:
     *   interest = ((loanAmount * interestPercentage) / 100) / 12
     *
     * Principal is whatever's left of the payment after interest, never
     * negative (a payment smaller than the fixed interest contributes ₹0
     * principal rather than going negative).
     */
    public static BigDecimal[] splitInterestAndPrincipal(
            BigDecimal amountPaid,
            BigDecimal loanAmount,
            BigDecimal interestPercentage
    ) {
        BigDecimal amount = amountPaid != null ? amountPaid : BigDecimal.ZERO;
        BigDecimal loan = loanAmount != null ? loanAmount : BigDecimal.ZERO;
        BigDecimal rate = interestPercentage != null ? interestPercentage : BigDecimal.ZERO;

        BigDecimal interest = loan
                .multiply(rate)
                .divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP)
                .divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);

        BigDecimal principal = amount.subtract(interest).max(BigDecimal.ZERO);

        return new BigDecimal[]{interest, principal};
    }

    /**
     * The total amount the member is expected to pay over the life of the
     * loan: weeklyAmount * totalWeeks when both are known, otherwise falls
     * back to loanAmount (for members created before totalWeeks existed, or
     * with a custom plan that doesn't specify a week count).
     */
    public static BigDecimal totalToCollect(BigDecimal weeklyAmount, Integer totalWeeks, BigDecimal loanAmount) {
        if (weeklyAmount != null && totalWeeks != null && totalWeeks > 0) {
            return weeklyAmount.multiply(BigDecimal.valueOf(totalWeeks));
        }
        return loanAmount != null ? loanAmount : BigDecimal.ZERO;
    }

    /**
     * Recomputes the Outstanding Amount from scratch: the total planned
     * collection for the loan (weeklyAmount * totalWeeks, or loanAmount as a
     * fallback) minus the principal portion of every payment recorded so
     * far. Never goes below zero.
     */
    public static BigDecimal calculate(
            BigDecimal weeklyAmount,
            Integer totalWeeks,
            BigDecimal loanAmount,
            BigDecimal interestPercentage,
            List<Payment> payments
    ) {
        BigDecimal totalToCollect = totalToCollect(weeklyAmount, totalWeeks, loanAmount);

        BigDecimal totalPrincipalCollected = BigDecimal.ZERO;
        if (payments != null) {
            for (Payment payment : payments) {
                BigDecimal principal = splitInterestAndPrincipal(
                        payment.getAmountPaid(),
                        loanAmount,
                        interestPercentage
                )[1];
                totalPrincipalCollected = totalPrincipalCollected.add(principal);
            }
        }

        return totalToCollect.subtract(totalPrincipalCollected).max(BigDecimal.ZERO);
    }

    /**
     * Same as {@link #calculate}, but for the "with interest" Outstanding
     * Amount: both the Interest portion AND the Principal portion of every
     * payment are deducted from the total-to-collect figure (whereas
     * {@link #calculate} only deducts the Principal portion).
     *
     *   outstandingWithInterest = totalToCollect - Σ(interest + principal) for every payment
     *
     * Note this deducts the fixed per-payment interest even on a payment
     * smaller than that interest amount (principal alone floors at ₹0, but
     * interest is always subtracted in full), consistent with interest being
     * a fixed charge independent of what was actually collected that week.
     */
    public static BigDecimal calculateWithInterest(
            BigDecimal weeklyAmount,
            Integer totalWeeks,
            BigDecimal loanAmount,
            BigDecimal interestPercentage,
            List<Payment> payments
    ) {
        BigDecimal totalToCollect = totalToCollect(weeklyAmount, totalWeeks, loanAmount);

        BigDecimal totalCollected = BigDecimal.ZERO;
        if (payments != null) {
            for (Payment payment : payments) {
                BigDecimal[] split = splitInterestAndPrincipal(
                        payment.getAmountPaid(),
                        loanAmount,
                        interestPercentage
                );
                BigDecimal interest = split[0];
                BigDecimal principal = split[1];
                totalCollected = totalCollected.add(interest).add(principal);
            }
        }

        return totalToCollect.subtract(totalCollected).max(BigDecimal.ZERO);
    }
}
