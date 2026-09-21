package com.microfinance.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MemberRequest {

    /**
     * Now provided by the admin at creation time instead of auto-generated.
     * Left optional on update requests (the code is immutable after creation;
     * MemberServiceImpl ignores this field when updating).
     */
    @Size(max = 20, message = "Member ID must not exceed 20 characters")
    @Pattern(regexp = "^$|^[A-Za-z0-9_-]+$", message = "Member ID can only contain letters, numbers, hyphens, and underscores")
    private String memberCode;

    @NotBlank(message = "Member name is required")
    @Size(max = 100, message = "Member name must not exceed 100 characters")
    private String name;

    private boolean headMember = false;

    /**
     * Required when headMember is false — the id of the existing head member
     * this member should be added under as a sub-member. Must be null when
     * headMember is true. Validated in MemberServiceImpl since it depends on
     * the value of another field.
     */
    private Long headMemberId;

    @Size(max = 50, message = "Group code must not exceed 50 characters")
    private String groupCode;
    
    @Size(max = 100, message = "Center place must not exceed 100 characters")
    private String centerPlace;

    @Size(max = 50, message = "Center code must not exceed 50 characters")
    private String centerCode;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^[6-9]\\d{9}$", message = "Phone number must be a valid 10-digit number")
    private String phoneNumber;

    @Pattern(regexp = "^$|^[6-9]\\d{9}$", message = "Alternate phone number must be a valid 10-digit number")
    private String alternatePhoneNumber;

    // ── Personal information ───────────────────────────────────────────────
    private String marriageStatus;

    @Pattern(regexp = "^$|^(MALE|FEMALE)$", message = "Gender must be MALE or FEMALE")
    private String gender;

    private LocalDate dateOfBirth;

    private String house;

    @NotBlank(message = "Address is required")
    @Size(max = 500, message = "Address must not exceed 500 characters")
    private String address;

    /** "FATHER" or "HUSBAND" — which relation fatherOrHusbandName refers to. */
    @Pattern(regexp = "^$|^(FATHER|HUSBAND)$", message = "Relation must be FATHER or HUSBAND")
    private String fatherOrHusbandRelation;

    @Size(max = 100, message = "Father/Husband name must not exceed 100 characters")
    private String fatherOrHusbandName;

    @Size(max = 200, message = "Purpose of loan must not exceed 200 characters")
    private String purposeOfLoan;

    // Identity documents
    @Size(max = 20, message = "Aadhaar number must not exceed 20 characters")
    private String aadhaarNumber;

    @Size(max = 20, message = "PAN number must not exceed 20 characters")
    private String panNumber;

    @Size(max = 30, message = "Voter ID must not exceed 30 characters")
    private String voterId;

    @Size(max = 30, message = "Smart card number must not exceed 30 characters")
    private String smartCardNumber;

    // Bank details
    @Size(max = 100, message = "Bank name must not exceed 100 characters")
    private String bankName;

    @Size(max = 30, message = "Bank account number must not exceed 30 characters")
    private String bankAccountNumber;

    @Pattern(regexp = "^$|^\\d{6,7}$", message = "Cheque number must be 6-7 digits")
    private String chequeNumber;

    // Nominee details
    @Size(max = 100, message = "Nominee name must not exceed 100 characters")
    private String nomineeName;

    @Pattern(regexp = "^$|^[6-9]\\d{9}$", message = "Nominee phone number must be a valid 10-digit number")
    private String nomineePhoneNumber;

    @Size(max = 50, message = "Nominee relation must not exceed 50 characters")
    private String nomineeRelation;

    @Size(max = 20, message = "Nominee Aadhaar must not exceed 20 characters")
    private String nomineeAadhaar;

    @Size(max = 20, message = "Nominee PAN must not exceed 20 characters")
    private String nomineePan;

    @Size(max = 30, message = "Nominee voter ID must not exceed 30 characters")
    private String nomineeVoterId;

    @Pattern(regexp = "^$|^(MALE|FEMALE)$", message = "Nominee gender must be MALE or FEMALE")
    private String nomineeGender;

    // Collection
    @NotNull(message = "Weekly amount is required")
    @DecimalMin(value = "0.01", message = "Weekly amount must be greater than zero")
    @Digits(integer = 10, fraction = 2, message = "Weekly amount has an invalid format")
    private BigDecimal weeklyAmount;

    @NotNull(message = "Join date is required")
    @PastOrPresent(message = "Join date cannot be in the future")
    private LocalDate joinDate;

    /**
     * Day of the week this member is scheduled for collection (MONDAY..SUNDAY).
     * Required; validated against the Weekday enum in MemberServiceImpl so an
     * invalid value returns a clear message instead of a generic 400.
     */
    @NotBlank(message = "Weekday is required")
    @Pattern(regexp = "(?i)^(MONDAY|TUESDAY|WEDNESDAY|THURSDAY|FRIDAY|SATURDAY|SUNDAY)$",
            message = "Weekday must be one of Monday, Tuesday, Wednesday, Thursday, Friday, Saturday, Sunday")
    private String weekday;

    @Size(max = 500, message = "Notes must not exceed 500 characters")
    private String notes;

    @DecimalMin(value = "0", message = "Loan amount cannot be negative")
    private BigDecimal loanAmount;

    /**
     * Total number of installments (weeks/months) for the selected Loan
     * Plan — e.g. 26 for the ₹15,000 weekly plan. Used with weeklyAmount to
     * work out the total amount to be collected for Outstanding Amount.
     * Optional; falls back to loanAmount when not provided.
     */
    @Min(value = 1, message = "Total weeks must be at least 1")
    private Integer totalWeeks;

    /**
     * Interest rate (%) applied to this member's loan — used to split every
     * collection into Interest + Principal so the Outstanding Amount can be
     * tracked. Optional; treated as 0% when not provided.
     */
    @DecimalMin(value = "0", message = "Interest percentage cannot be negative")
    @DecimalMax(value = "100", message = "Interest percentage cannot exceed 100")
    @Digits(integer = 3, fraction = 2, message = "Interest percentage has an invalid format")
    private BigDecimal interestPercentage;

    /** "WEEKLY" or "MONTHLY", from the selected Loan Plan. Defaults to WEEKLY if not provided. */
    private String paymentFrequency;

    @DecimalMin(value = "0", message = "Insurance amount cannot be negative")
    private BigDecimal insuranceAmount;

    @DecimalMin(value = "0", message = "Processing amount cannot be negative")
    private BigDecimal processingAmount;

    /**
     * Optional id of the staff member (field/collection agent) to assign to
     * this customer. Admin-only in the UI — MemberFormDialog only renders and
     * sends this field when the logged-in user is ADMIN; STAFF/VIEWER submit
     * requests never include it, leaving any existing assignment untouched
     * on update, or unset on create.
     */
    private Long staffMemberId;
}
