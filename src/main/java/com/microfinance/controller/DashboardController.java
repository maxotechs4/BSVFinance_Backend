package com.microfinance.controller;

import com.microfinance.dto.response.ApiResponse;
import com.microfinance.dto.response.DashboardChartsResponse;
import com.microfinance.dto.response.DashboardSummaryResponse;
import com.microfinance.dto.response.GroupSummaryResponse;
import com.microfinance.dto.response.InsuranceProcessingSummaryResponse;
import com.microfinance.service.DashboardService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Not part of the originally specified endpoint list, but added to back the
 * "Weekly Collection Summary" cards and "Dashboard Charts" sections of the frontend
 * spec, which need server-computed aggregates rather than raw member/payment lists.
 */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Summary cards and chart data")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<DashboardSummaryResponse>> getSummary() {
        return ResponseEntity.ok(ApiResponse.success(dashboardService.getSummary()));
    }

    @GetMapping("/charts")
    public ResponseEntity<ApiResponse<DashboardChartsResponse>> getCharts() {
        return ResponseEntity.ok(ApiResponse.success(dashboardService.getCharts()));
    }

    @GetMapping("/groups")
    public ResponseEntity<ApiResponse<List<GroupSummaryResponse>>> getGroupSummaries() {
        return ResponseEntity.ok(ApiResponse.success(dashboardService.getGroupSummaries()));
    }

    @GetMapping("/insurance-processing-summary")
    public ResponseEntity<ApiResponse<InsuranceProcessingSummaryResponse>> getInsuranceProcessingSummary() {
        return ResponseEntity.ok(ApiResponse.success(dashboardService.getInsuranceProcessingSummary()));
    }
}
