package com.microfinance.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Backs the "Insurance & Processing Collection" cards shown on the
 * Dashboard, below the Groups section. Insurance and processing amounts
 * are one-time charges recorded once per member (set from the create/edit
 * member form), so these figures are simple aggregates across all members
 * rather than per-week values.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InsuranceProcessingSummaryResponse {

    /** Members who have a non-zero insurance and/or processing amount recorded. */
    private long membersPaidCount;

    /** Sum of insuranceAmount across all members. */
    private BigDecimal totalInsuranceCollected;

    /** Sum of processingAmount across all members. */
    private BigDecimal totalProcessingCollected;

    /** totalInsuranceCollected + totalProcessingCollected. */
    private BigDecimal totalCollected;
}
