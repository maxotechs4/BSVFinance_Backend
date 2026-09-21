package com.microfinance.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Singleton row (always id = 1) tracking the admin's capital fund.
 *
 * - capitalAmount: the amount currently available to fund new loans. Topped
 *   up by the admin from the "Capital Amount" box on the Admin page (each
 *   entry ADDS to the existing amount), and drawn down automatically by new
 *   member loans (see CapitalServiceImpl.drawForNewLoan). Income/Expense
 *   entries do NOT touch this field — see collectionUtilized below.
 * - collectionUtilized: running total of how much of the "Total Collection"
 *   pool is currently unavailable — increased when a loan's amount exceeds
 *   available capitalAmount (the shortfall is drawn from collection), and
 *   also increased/decreased directly by EXPENSE/INCOME entries. Used to
 *   compute how much of the total collection is still actually free (see
 *   CapitalServiceImpl.toResponse()).
 */
@Entity
@Table(name = "capital_account")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CapitalAccount {

    @Id
    private Long id;

    @Column(name = "capital_amount", precision = 14, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal capitalAmount = BigDecimal.ZERO;

    @Column(name = "collection_utilized", precision = 14, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal collectionUtilized = BigDecimal.ZERO;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    private void touch() {
        this.updatedAt = LocalDateTime.now();
    }
}
