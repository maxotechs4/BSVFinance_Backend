package com.microfinance.dto.response;

import com.microfinance.entity.enums.MemberStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SavingsMemberResponse {

    private Long id;
    private String memberCode;
    private String name;
    private String place;
    private String phoneNumber;
    private String alternatePhoneNumber;
    private BigDecimal monthlyAmount;
    private Integer totalMonths;
    private LocalDate joinDate;
    private MemberStatus status;

    // Enrichment - cumulative across all 36 installments
    private int installmentsPaid;
    private BigDecimal totalPaid;
    private BigDecimal totalExpected;
    private BigDecimal totalBalance;

    private LocalDateTime createdAt;
}
