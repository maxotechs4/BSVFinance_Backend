package com.microfinance.service;

import com.microfinance.dto.response.MonthlyReportResponse;
import com.microfinance.dto.response.WeeklyReportResponse;
import com.microfinance.dto.response.YearlyReportResponse;
import com.microfinance.entity.enums.PaymentMethod;

public interface ReportService {
    WeeklyReportResponse getWeeklyReport(Integer weekNumber, Integer year, Long memberId, PaymentMethod method);
    MonthlyReportResponse getMonthlyReport(Integer month, Integer year, Long memberId, PaymentMethod method);
    YearlyReportResponse getYearlyReport(Integer year, Long memberId, PaymentMethod method);
}
