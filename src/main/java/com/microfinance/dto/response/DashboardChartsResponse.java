package com.microfinance.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardChartsResponse {

    /** Last 8 weeks of total collection, oldest first */
    private List<String> weeklyLabels;
    private List<BigDecimal> weeklyCollection;

    /** Last 6 months of total collection, oldest first */
    private List<String> monthlyLabels;
    private List<BigDecimal> monthlyCollection;

    private BigDecimal cashTotal;
    private BigDecimal onlineTotal;

    /** Top members by outstanding remaining amount */
    private List<String> remainingMemberLabels;
    private List<BigDecimal> remainingAmounts;
}
