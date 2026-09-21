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
public class MonthlyReportResponse {
    private Integer month;
    private Integer year;
    private BigDecimal totalCollection;
    private BigDecimal cashCollection;
    private BigDecimal onlineCollection;
    private BigDecimal totalDue;
    private List<WeeklyBreakdown> weeklyBreakdown;
    private List<PaymentResponse> payments;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class WeeklyBreakdown {
        private Integer weekNumber;
        private BigDecimal totalCollection;
    }
}
