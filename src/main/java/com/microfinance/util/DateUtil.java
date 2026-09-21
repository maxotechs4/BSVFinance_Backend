package com.microfinance.util;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.IsoFields;
import java.time.temporal.TemporalAdjusters;

public final class DateUtil {

    private DateUtil() {
    }

    public static int currentIsoWeek() {
        return LocalDate.now().get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
    }

    public static int currentIsoWeekYear() {
        return LocalDate.now().get(IsoFields.WEEK_BASED_YEAR);
    }

    public static LocalDate firstDayOfMonth(int year, int month) {
        return LocalDate.of(year, month, 1);
    }

    public static LocalDate lastDayOfMonth(int year, int month) {
        return LocalDate.of(year, month, 1).with(TemporalAdjusters.lastDayOfMonth());
    }

    public static LocalDate firstDayOfYear(int year) {
        return LocalDate.of(year, 1, 1);
    }

    public static LocalDate lastDayOfYear(int year) {
        return LocalDate.of(year, 12, 31);
    }

    public static long weeksElapsedSince(LocalDate joinDate) {
        if (joinDate == null || joinDate.isAfter(LocalDate.now())) {
            return 0;
        }
        return ChronoUnit.WEEKS.between(joinDate, LocalDate.now()) + 1;
    }

    /**
     * Determines the next week/year a member's collection should be recorded under,
     * based on the last week number and year that were actually recorded for them
     * (NOT the calendar's current ISO week). Pass {@code null} for both when the
     * member has no payment history yet - week 1 of the current calendar year is used.
     *
     * @return a 2-element array: [nextWeekNumber, nextPaymentYear]
     */
    public static int[] nextCollectionPeriod(Integer lastWeekNumber, Integer lastPaymentYear) {
        if (lastWeekNumber == null || lastPaymentYear == null) {
            return new int[]{1, currentIsoWeekYear()};
        }
        if (lastWeekNumber >= 53) {
            return new int[]{1, lastPaymentYear + 1};
        }
        return new int[]{lastWeekNumber + 1, lastPaymentYear};
    }
}