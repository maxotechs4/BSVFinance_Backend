package com.microfinance.entity;

import com.microfinance.entity.enums.PaymentMethod;
import com.microfinance.entity.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments", uniqueConstraints = {
        @UniqueConstraint(columnNames = "receipt_number"),
        @UniqueConstraint(columnNames = {"member_id", "week_number", "payment_year"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "receipt_number", nullable = false, length = 30)
    private String receiptNumber;

    @Column(name = "week_number", nullable = false)
    private Integer weekNumber;

    @Column(name = "payment_year", nullable = false)
    private Integer paymentYear;

    /** Snapshot of the member's weekly fixed amount at the time of payment */
    @Column(name = "weekly_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal weeklyAmount;

    @Column(name = "amount_paid", nullable = false, precision = 12, scale = 2)
    private BigDecimal amountPaid;

    @Column(name = "remaining_amount", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal remainingAmount = BigDecimal.ZERO;

    @Column(name = "extra_amount", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal extraAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 20)
    private PaymentMethod paymentMethod;

    @Column(name = "upi_transaction_id", length = 50)
    private String upiTransactionId;

    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate;

    @Column(length = 500)
    private String remarks;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
