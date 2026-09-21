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

/** One monthly installment payment against a SavingsMember's 36-month plan. */
@Entity
@Table(name = "savings_payments", uniqueConstraints = {
        @UniqueConstraint(columnNames = "receipt_number"),
        @UniqueConstraint(columnNames = {"savings_member_id", "installment_number"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SavingsPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "savings_member_id", nullable = false)
    private SavingsMember savingsMember;

    @Column(name = "receipt_number", nullable = false, length = 30)
    private String receiptNumber;

    /** Which of the 36 installments this payment is for (1-based). */
    @Column(name = "installment_number", nullable = false)
    private Integer installmentNumber;

    /** Snapshot of the member's monthly amount at the time of payment */
    @Column(name = "monthly_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal monthlyAmount;

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
