package com.microfinance.controller;

import com.microfinance.dto.response.ApiResponse;
import com.microfinance.dto.response.MonthlyReportResponse;
import com.microfinance.dto.response.WeeklyReportResponse;
import com.microfinance.dto.response.YearlyReportResponse;
import com.microfinance.entity.enums.PaymentMethod;
import com.microfinance.service.ReportService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Tag(name = "Reports", description = "Weekly, monthly, and yearly collection reports")
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/weekly")
    public ResponseEntity<ApiResponse<WeeklyReportResponse>> weeklyReport(
            @RequestParam(required = false) Integer weekNumber,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Long memberId,
            @RequestParam(required = false) PaymentMethod method) {
        return ResponseEntity.ok(ApiResponse.success(reportService.getWeeklyReport(weekNumber, year, memberId, method)));
    }

    @GetMapping("/monthly")
    public ResponseEntity<ApiResponse<MonthlyReportResponse>> monthlyReport(
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Long memberId,
            @RequestParam(required = false) PaymentMethod method) {
        return ResponseEntity.ok(ApiResponse.success(reportService.getMonthlyReport(month, year, memberId, method)));
    }

    @GetMapping("/yearly")
    public ResponseEntity<ApiResponse<YearlyReportResponse>> yearlyReport(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Long memberId,
            @RequestParam(required = false) PaymentMethod method) {
        return ResponseEntity.ok(ApiResponse.success(reportService.getYearlyReport(year, memberId, method)));
    }
}
