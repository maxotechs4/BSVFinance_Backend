package com.microfinance.entity;

import com.microfinance.entity.enums.MemberStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * A member enrolled in the fixed Monthly Savings scheme (₹1180/month for 36
 * months by default). Deliberately separate from Member/Payment — this is a
 * distinct, simpler product with no head/sub-member hierarchy, documents,
 * bank details, or nominee info.
 */
@Entity
@Table(name = "savings_members", uniqueConstraints = {
        @UniqueConstraint(columnNames = "member_code"),
        @UniqueConstraint(columnNames = "phone_number")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SavingsMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_code", nullable = false, length = 20)
    private String memberCode;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 100)
    private String place;

    @Column(name = "phone_number", nullable = false, length = 15)
    private String phoneNumber;

    @Column(name = "alternate_phone_number", length = 15)
    private String alternatePhoneNumber;

    /** Fixed monthly savings amount - defaults to the standard ₹1180 plan. */
    @Column(name = "monthly_amount", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal monthlyAmount = new BigDecimal("1180");

    /** Fixed term length in months - the standard plan is 36 months. */
    @Column(name = "total_months", nullable = false)
    @Builder.Default
    private Integer totalMonths = 36;

    @Column(name = "join_date", nullable = false)
    private LocalDate joinDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private MemberStatus status = MemberStatus.ACTIVE;

    @OneToMany(mappedBy = "savingsMember", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<SavingsPayment> payments = new ArrayList<>();

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
}
