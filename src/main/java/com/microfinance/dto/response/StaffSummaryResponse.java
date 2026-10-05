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
public class StaffSummaryResponse {
    private Long staffId;
    private String staffName;
    private long activeClients;
    private BigDecimal outstandingAmount;
    private BigDecimal collectionAmount;
    private BigDecimal parAmount;
    private BigDecimal pendingAmount;
}