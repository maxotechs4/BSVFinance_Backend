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
public class YearlyReportResponse {
    private Integer year;
    private BigDecimal totalCollection;
    private BigDecimal cashCollection;
    private BigDecimal onlineCollection;
    private BigDecimal totalDue;
    private List<MonthlyBreakdown> monthlyBreakdown;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MonthlyBreakdown {
        private Integer month;
        private BigDecimal totalCollection;
    }
}
