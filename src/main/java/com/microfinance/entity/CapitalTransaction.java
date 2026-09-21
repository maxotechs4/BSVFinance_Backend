package com.microfinance.entity;

import com.microfinance.entity.enums.CapitalTransactionType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * One manual Income or Expense entry recorded by the admin from the Entry
 * Form on the Admin page. Every entry adjusts CapitalAccount.collectionUtilized
 * (- for INCOME, which frees up/boosts available collection; + for EXPENSE,
 * which draws more from it) when created/updated/deleted. capitalAmount is
 * never touched by entries — only by the Capital Amount box and new loans.
 */
@Entity
@Table(name = "capital_transactions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CapitalTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private CapitalTransactionType type;

    @Column(name = "amount", precision = 14, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(name = "purpose", length = 255)
    private String purpose;

    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    private void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.transactionDate == null) {
            this.transactionDate = now.toLocalDate();
        }
    }

    @PreUpdate
    private void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
