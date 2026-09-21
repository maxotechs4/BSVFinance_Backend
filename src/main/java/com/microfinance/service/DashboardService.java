package com.microfinance.service;

import com.microfinance.dto.response.DashboardChartsResponse;
import com.microfinance.dto.response.DashboardSummaryResponse;
import com.microfinance.dto.response.GroupSummaryResponse;
import com.microfinance.dto.response.InsuranceProcessingSummaryResponse;

import java.util.List;

public interface DashboardService {
    DashboardSummaryResponse getSummary();
    DashboardChartsResponse getCharts();

    /** One row per head member, with their group's member count and this week's collection. */
    List<GroupSummaryResponse> getGroupSummaries();

    /** Aggregate one-time insurance/processing charges collected across all members. */
    InsuranceProcessingSummaryResponse getInsuranceProcessingSummary();
}
