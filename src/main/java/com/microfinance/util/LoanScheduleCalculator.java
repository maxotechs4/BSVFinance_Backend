package com.microfinance.util;

import com.microfinance.dto.response.LoanInstallmentResponse;
import com.microfinance.entity.Member;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Builds the printed "Loan Card / Repayment Schedule" — a full,
 * reducing-balance amortization table — from a member's loan terms
 * (loanAmount, totalWeeks, interestPercentage, weeklyAmount, joinDate).
 *
 * This is a weekly collection schedule: demand dates advance 7 days at a
 * time from joinDate, so every installment falls on the same weekday (e.g.
 * joinDate = Wed 23 Sept 2026 → next installment Wed 30 Sept 2026 → Wed
 * 07 Oct 2026, and so on).
 *
 * The Principal / Interest / Total columns are still computed here (reducing
 * balance: interest on the outstanding balance, principal as the remainder
 * of the fixed installment) in case they're useful elsewhere, but the print
 * page currently renders those three columns blank on purpose — staff fill
 * them in by hand on the printed sheet, matching how the original paper
 * form works.
 *
 * NOTE: this only produces the *demand* (expected) side of the schedule.
 * The collected* / collectionDate / receiptNo fields are left null here —
 * matching actual Payment rows to a specific installment number depends on
 * your Payment entity's fields, which weren't available when this was
 * written. Wire that up as a second pass once you share Payment.java, or
 * leave it blank and have staff fill it in by hand on the printed sheet
 * (which is how the original paper form works anyway).
 */
public final class LoanScheduleCalculator {

    private LoanScheduleCalculator() {}

    public static List<LoanInstallmentResponse> build(Member member) {
        List<LoanInstallmentResponse> schedule = new ArrayList<>();

        BigDecimal principalRemaining = member.getLoanAmount();
        Integer totalInstallments = member.getTotalWeeks();
        BigDecimal installmentAmount = member.getWeeklyAmount();
        BigDecimal annualRate = member.getInterestPercentage();
        LocalDate startDate = member.getJoinDate();
        boolean isMonthly = "MONTHLY".equalsIgnoreCase(member.getPaymentFrequency());

        if (principalRemaining == null || totalInstallments == null || totalInstallments <= 0
                || installmentAmount == null || startDate == null) {
            return schedule; // not enough loan data yet — print page just hides this section
        }
        if (annualRate == null) {
            annualRate = BigDecimal.ZERO;
        }

        // Periodic rate: annual% / 100, divided by periods-per-year (12 for
        // monthly, 52 for weekly). Kept separate from the date logic below,
        // since the printed card always shows monthly demand dates
        // regardless of the loan's weekly/monthly repayment frequency.
        BigDecimal periodsPerYear = isMonthly ? BigDecimal.valueOf(12) : BigDecimal.valueOf(52);
        BigDecimal periodicRate = annualRate
                .divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP)
                .divide(periodsPerYear, 10, RoundingMode.HALF_UP);

        for (int i = 1; i <= totalInstallments; i++) {
            // Demand dates advance 7 days at a time starting FROM joinDate
            // itself (installment 1 = joinDate, e.g. Wed 23 Sept 2026),
            // then Wed 30 Sept 2026, Wed 07 Oct 2026, and so on — every
            // installment lands on the same weekday as joinDate.
            LocalDate demandDate = startDate.plusWeeks(i - 1);

            BigDecimal interest = principalRemaining
                    .multiply(periodicRate)
                    .setScale(2, RoundingMode.HALF_UP);

            BigDecimal principalPortion;
            BigDecimal totalPortion;

            if (i == totalInstallments) {
                // Last installment: pay off whatever principal is left exactly,
                // so the schedule never drifts from the loan amount by a few
                // rupees due to rounding on every prior row.
                principalPortion = principalRemaining;
                totalPortion = principalPortion.add(interest).setScale(2, RoundingMode.HALF_UP);
            } else {
                totalPortion = installmentAmount.setScale(2, RoundingMode.HALF_UP);
                principalPortion = totalPortion.subtract(interest).setScale(2, RoundingMode.HALF_UP);
                // Guard against a mis-configured plan where interest alone
                // would exceed the fixed installment amount.
                if (principalPortion.compareTo(BigDecimal.ZERO) < 0) {
                    principalPortion = BigDecimal.ZERO;
                    totalPortion = interest;
                }
            }

            schedule.add(LoanInstallmentResponse.builder()
                    .installmentNo(i)
                    .demandDate(demandDate)
                    .demandPrincipal(principalPortion)
                    .demandInterest(interest)
                    .demandTotal(totalPortion)
                    .build());

            principalRemaining = principalRemaining.subtract(principalPortion);
            if (principalRemaining.compareTo(BigDecimal.ZERO) < 0) {
                principalRemaining = BigDecimal.ZERO;
            }
        }

        return schedule;
    }
}