package com.microfinance.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardSummaryResponse {
    private long totalMembers;
    private long activeMembers;
    private Integer currentWeekNumber;
    private Integer currentYear;
    private BigDecimal totalCollectionThisWeek;
    private BigDecimal cashCollection;
    private BigDecimal onlineCollection;
    private BigDecimal remainingDue;
    private BigDecimal totalOutstanding;

    // All-time cumulative totals, across every week ever recorded (not just this week)
    // All-time cumulative totals, across every week ever recorded (not just this week)
    private BigDecimal totalCollectionAllTime;
    private BigDecimal totalOutstandingAllTime;
    private BigDecimal cashCollectionAllTime;
    private BigDecimal onlineCollectionAllTime;
}
