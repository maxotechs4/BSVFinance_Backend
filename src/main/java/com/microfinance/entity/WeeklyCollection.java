package com.microfinance.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Materialized aggregate of all payments belonging to one (weekNumber, paymentYear) bucket.
 * Recomputed by PaymentService every time a payment in that week is created, updated, or
 * deleted, so dashboard and report reads avoid re-aggregating the full payments table.
 */
@Entity
@Table(name = "weekly_collections", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"week_number", "collection_year"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WeeklyCollection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "week_number", nullable = false)
    private Integer weekNumber;

    @Column(name = "collection_year", nullable = false)
    private Integer collectionYear;

    @Column(name = "total_collection", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal totalCollection = BigDecimal.ZERO;

    @Column(name = "cash_collection", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal cashCollection = BigDecimal.ZERO;

    @Column(name = "online_collection", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal onlineCollection = BigDecimal.ZERO;

    @Column(name = "total_due", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal totalDue = BigDecimal.ZERO;

    @Column(name = "members_paid", nullable = false)
    @Builder.Default
    private Integer membersPaid = 0;

    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void onSave() {
        this.updatedAt = LocalDateTime.now();
    }
}
