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
public class WeeklyReportResponse {
    private Integer weekNumber;
    private Integer year;
    private BigDecimal totalCollection;
    private BigDecimal cashCollection;
    private BigDecimal onlineCollection;
    private BigDecimal totalDue;
    private long membersPaid;
    private List<PaymentResponse> payments;
}
