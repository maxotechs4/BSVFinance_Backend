package com.microfinance.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Per-group (head + their sub-members) collection summary for the
 * "Groups" section of the dashboard.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroupSummaryResponse {

    private Long headId;

    private String headMemberCode;

    private String headName;

    private String centerPlace;

    private String groupId;

    private String groupName;

    /** Head + all their sub-members */
    private int totalMembers;

    private BigDecimal totalCollectedThisWeek;

    private BigDecimal totalExpectedThisWeek;

    private BigDecimal remainingThisWeek;

    /**
     * Number of weeks for which every member in this group
     * has completed their weekly payment.
     */
    private int billGenerated;
}