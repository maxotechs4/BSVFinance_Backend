package com.microfinance.entity;

import com.microfinance.entity.enums.MemberStatus;
import com.microfinance.entity.enums.Weekday;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "members", uniqueConstraints = {
        @UniqueConstraint(columnNames = "member_code")
})
// Note: phone_number is intentionally NOT unique at the DB level — a person
// whose loan has been CLOSED can get a brand new member profile (new
// id/memberCode) for their next loan, reusing the same phone number. The
// service layer (MemberServiceImpl.createMember) still blocks a duplicate
// phone number while an existing profile's loan is not CLOSED.
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_code", nullable = false, length = 20)
    private String memberCode;

    @Column(nullable = false, length = 100)
    private String name;

    /** Whether this member is the head/leader of their group */
    @Column(name = "is_head_member", nullable = false)
    @Builder.Default
    private boolean headMember = false;

    /**
     * The head member this member belongs to (null for a head member itself).
     * Self-referencing relationship: currently only one level deep
     * (head -> sub-members) is enforced by the service layer, but the
     * model allows a member to have both a parentHead and subMembers so
     * multi-level grouping can be introduced later without a schema change.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_head_id")
    private Member parentHead;

    @OneToMany(mappedBy = "parentHead")
    @Builder.Default
    private List<Member> subMembers = new ArrayList<>();

    @Column(name = "group_code", length = 50)
    private String groupCode;

    @Column(name = "center_place", length = 100)
    private String centerPlace;

    @Column(name = "group_id", length = 50)
    private String groupId;

    @Column(name = "group_name", length = 100)
    private String groupName;

    @Column(name = "phone_number", nullable = false, length = 15)
    private String phoneNumber;

    /** Optional secondary/alternate contact number */
    @Column(name = "alternate_phone_number", length = 15)
    private String alternatePhoneNumber;

    // ── Personal information ───────────────────────────────────────────────
    @Column(name = "marriage_status", length = 20)
    private String marriageStatus;

    @Column(name = "gender", length = 10)
    private String gender;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "house", length = 20)
    private String house;

    @Column(length = 500)
    private String address;

    /** "FATHER" or "HUSBAND" — which relation fatherOrHusbandName refers to. */
    @Column(name = "father_or_husband_relation", length = 10)
    private String fatherOrHusbandRelation;

    @Column(name = "father_or_husband_name", length = 100)
    private String fatherOrHusbandName;

    /** Stated purpose of the loan, shown on the printed Loan Application. */
    @Column(name = "purpose_of_loan", length = 200)
    private String purposeOfLoan;

    // ── Identity documents ──────────────────────────────────────────────────
    @Column(name = "aadhaar_number", length = 20)
    private String aadhaarNumber;

    @Column(name = "pan_number", length = 20)
    private String panNumber;

    @Column(name = "voter_id", length = 30)
    private String voterId;

    @Column(name = "smart_card_number", length = 30)
    private String smartCardNumber;

    // ── Bank details ────────────────────────────────────────────────────────
    @Column(name = "bank_name", length = 100)
    private String bankName;

    @Column(name = "bank_account_number", length = 30)
    private String bankAccountNumber;

    @Column(name = "cheque_number", length = 10)
    private String chequeNumber;

    // ── Nominee details ─────────────────────────────────────────────────────
    @Column(name = "nominee_name", length = 100)
    private String nomineeName;

    @Column(name = "nominee_phone_number", length = 15)
    private String nomineePhoneNumber;

    @Column(name = "nominee_relation", length = 50)
    private String nomineeRelation;

    @Column(name = "nominee_aadhaar", length = 20)
    private String nomineeAadhaar;

    @Column(name = "nominee_pan", length = 20)
    private String nomineePan;

    @Column(name = "nominee_voter_id", length = 30)
    private String nomineeVoterId;

    @Column(name = "nominee_gender", length = 10)
    private String nomineeGender;

    /**
     * Nominee photo(s). Multi-image support — a member can have any number of
     * uploaded photos, each a separate {@link NomineeImage} row (this used to
     * be three columns — nominee_image_data/content_type/file_name — directly
     * on this table for a single photo; see database/004_nominee_images_table.sql
     * for the migration to this table). Uploading is Admin-only (see
     * SecurityConfig); viewing/downloading an existing one is available to
     * every role via MemberController's GET endpoints.
     */
    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<NomineeImage> nomineeImages = new ArrayList<>();
    
    
    @Column(name = "member_photo_data")
    private byte[] memberPhotoData;

    @Column(name = "member_photo_content_type", length = 50)
    private String memberPhotoContentType;

    @Column(name = "member_photo_file_name", length = 255)
    private String memberPhotoFileName;

    // ── Loan / collection ────────────────────────────────────────────────────
    @Column(name = "weekly_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal weeklyAmount;

    @Column(name = "join_date", nullable = false)
    private LocalDate joinDate;

    /** Day of the week this member is scheduled for collection visits/round. */
    @Enumerated(EnumType.STRING)
    @Column(name = "weekday", length = 10)
    private Weekday weekday;

    @Column(length = 500)
    private String notes;

    @Column(name = "credit_balance", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal creditBalance = BigDecimal.ZERO;

    @Column(name = "insurance_amount", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal insuranceAmount = BigDecimal.ZERO;

    /** One-time loan processing fee, set only from the Admin page. */
    @Column(name = "processing_amount", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal processingAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private MemberStatus status = MemberStatus.ACTIVE;

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Payment> payments = new ArrayList<>();

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    @Column(name = "loan_amount", precision = 12, scale = 2)
    private BigDecimal loanAmount;

    /**
     * Total number of weekly/monthly installments for this member's loan
     * plan (e.g. 26 for the ₹15,000 weekly plan). Set from the Loan Plan
     * selected on the Create Member form. Used together with weeklyAmount to
     * work out the total amount the member is expected to pay over the life
     * of the loan — see {@link com.microfinance.util.OutstandingCalculator}.
     * Nullable for members created before this feature, or on a custom plan
     * that doesn't specify a week count.
     */
    @Column(name = "total_weeks")
    private Integer totalWeeks;

    /**
     * Interest rate (%) applied to every collection for this member's loan.
     * Used to split each payment into an Interest portion and a Principal
     * portion — see {@link com.microfinance.util.OutstandingCalculator}.
     * Nullable so existing members created before this feature are
     * unaffected (treated as 0% until an admin sets it).
     */
    @Column(name = "interest_percentage", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal interestPercentage = BigDecimal.ZERO;

    /**
     * Current Outstanding Amount: the total amount the member is expected to
     * pay over the life of the loan (weeklyAmount * totalWeeks, falling back
     * to loanAmount if totalWeeks isn't set) minus the principal portion of
     * every payment collected so far (interest portions don't reduce it).
     * Recalculated by the service layer after every payment
     * create/update/delete and whenever loanAmount, totalWeeks, or
     * interestPercentage changes on the member. Nullable for members created
     * before this feature — treated as unknown until recalculated.
     */
    @Column(name = "outstanding_amount", precision = 12, scale = 2)
    private BigDecimal outstandingAmount;

    /**
     * Current Outstanding Amount (with interest): the total amount expected
     * to be collected for this loan (weeklyAmount * totalWeeks, falling back
     * to loanAmount) minus BOTH the interest and principal portions of every
     * payment collected so far — unlike {@link #outstandingAmount}, which
     * only deducts principal. Recalculated at the same times as
     * outstandingAmount — see {@link com.microfinance.util.OutstandingCalculator#calculateWithInterest}.
     * Nullable for members created before this feature — treated as unknown
     * until recalculated.
     */
    @Column(name = "outstanding_amount_with_interest", precision = 12, scale = 2)
    private BigDecimal outstandingAmountWithInterest;

    /** "WEEKLY" or "MONTHLY", set from the Loan Plan selected at creation. Defaults to WEEKLY for older members. */
    @Column(name = "payment_frequency", length = 10)
    @Builder.Default
    private String paymentFrequency = "WEEKLY";

    /**
     * Field/collection staff assigned to this member at creation. Optional —
     * nullable so existing members and members created without an assignment
     * are unaffected. ON DELETE SET NULL means deleting a staff record from
     * the Staff tab never fails or cascades just because members reference it
     * — it simply unassigns them. Admin-only end to end (see SecurityConfig
     * and MemberFormDialog on the frontend).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "staff_member_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private StaffMember staffMember;


        /**
     * How much of this member's loan was funded from Capital vs from the
     * Total Collection pool, recorded at creation time (see
     * CapitalService.drawForNewLoan). Used to precisely reverse the draw if
     * this member is ever deleted — without this, deleting a member would
     * leave its capital/collection deduction permanently stuck in the
     * ledger forever, since the singleton CapitalAccount row has no other
     * way to know how much to give back.
     */
    @Column(name = "capital_funded_amount", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal capitalFundedAmount = BigDecimal.ZERO;

    @Column(name = "collection_funded_amount", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal collectionFundedAmount = BigDecimal.ZERO;
}
