package com.microfinance.dto.response;
import java.util.List;
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
public class MemberResponse {
    private Long id;
    private String memberCode;
    private String name;
    private boolean headMember;

    /** Id and name of the head member this member belongs to (null for a head member itself) */
    private Long headMemberId;
    private String headMemberName;

    /** Count of sub-members currently linked under this member (0 for a non-head member) */
    private int subMemberCount;

    private String groupCode;
    private String centerPlace;
    private String groupId;
    private String groupName;
    private String phoneNumber;
    private String alternatePhoneNumber;
    private String marriageStatus;
    private String gender;
    private LocalDate dateOfBirth;
    private String house;
    private String address;
    private String fatherOrHusbandRelation;
    private String fatherOrHusbandName;
    private String purposeOfLoan;

    // Identity documents
    private String aadhaarNumber;
    private String panNumber;
    private String voterId;
    private String smartCardNumber;

    // Bank details
    private String bankName;
    private String bankAccountNumber;
    private String chequeNumber;

    // Nominee details
    private String nomineeName;
    private String nomineePhoneNumber;
    private String nomineeRelation;
    private String nomineeAadhaar;
    private String nomineePan;
    private String nomineeVoterId;
    private String nomineeGender;

    /** True if at least one nominee photo has been uploaded (raw bytes never travel in this DTO). */
    private boolean hasNomineeImage;

    /** Number of nominee photos uploaded for this member (multi-image support). Fetch the list via GET /members/{id}/nominee-images. */
    private int nomineeImageCount;

    // Collection
    private BigDecimal weeklyAmount;
    private LocalDate joinDate;
    private String weekday;
    private String notes;
    private BigDecimal creditBalance;
    private MemberStatus status;
    private String paymentFrequency;

    // Admin-only charges
    private BigDecimal insuranceAmount;
    private BigDecimal processingAmount;

    // Assigned staff (field/collection agent) — admin-only in the UI
    private Long staffMemberId;
    private String staffMemberName;

    // Current week enrichment
    private BigDecimal currentWeekPaid;
    private BigDecimal remainingAmount;
    private String currentWeekStatus;
    private String lastPaymentMethod;
    private LocalDate lastPaymentDate;

    // Next collection (Collection page "Week" column): the week that will be recorded
    // when a new collection amount is saved for this member - their last recorded
    // week + 1 (NOT the calendar's current week). If that week was already saved
    // (e.g. reopening the page), the existing amount/method are echoed back too.
    private Integer nextCollectionWeek;
    private Integer nextCollectionYear;
    private BigDecimal nextCollectionAmountPaid;
    private String nextCollectionMethod;

    private LocalDateTime createdAt;

    private BigDecimal loanAmount;

    /** Total number of installments (weeks/months) for the loan plan, e.g. 26. */
    private Integer totalWeeks;

    private BigDecimal totalExpected;
    private BigDecimal totalPaid;
    private BigDecimal totalBalance;

    /** Interest rate (%) applied to this member's loan. */
    private BigDecimal interestPercentage;

    /**
     * Current Outstanding Amount: the total amount expected to be collected
     * for this loan (weeklyAmount * totalWeeks, falling back to loanAmount)
     * minus principal collected so far. See OutstandingCalculator.
     */
    private BigDecimal outstandingAmount;

    /**
     * Current Outstanding Amount (with interest): same as outstandingAmount,
     * but both the interest and principal portions of every payment are
     * deducted. See OutstandingCalculator#calculateWithInterest.
     */
    private BigDecimal outstandingAmountWithInterest;
    private List<LoanInstallmentResponse> loanSchedule;
    /** True if a photo has been uploaded — raw bytes never travel in this DTO. */
    private boolean hasMemberPhoto;
}