package com.microfinance.repository;
import com.microfinance.entity.enums.MemberStatus;
import com.microfinance.entity.Payment;
import com.microfinance.entity.enums.PaymentMethod;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByReceiptNumber(String receiptNumber);

    boolean existsByReceiptNumber(String receiptNumber);

    Optional<Payment> findByMember_IdAndWeekNumberAndPaymentYear(
            Long memberId,
            Integer weekNumber,
            Integer paymentYear
    );

    Optional<Payment> findTopByMember_IdOrderByPaymentYearDescWeekNumberDesc(
            Long memberId
    );

    List<Payment> findByMember_IdOrderByPaymentYearDescWeekNumberDesc(
            Long memberId
    );

    void deleteByMember_Id(Long memberId);

    Page<Payment> findByMember_IdOrderByPaymentYearDescWeekNumberDesc(
            Long memberId,
            Pageable pageable
    );

    List<Payment> findByWeekNumberAndPaymentYear(
            Integer weekNumber,
            Integer paymentYear
    );

    long countByWeekNumberAndPaymentYear(
            Integer weekNumber,
            Integer paymentYear
    );

    List<Payment> findByMember_IdIn(
            List<Long> memberIds
    );

    Optional<Payment> findTopByReceiptNumberStartingWithOrderByReceiptNumberDesc(String prefix);
    
    @Query("""
            SELECT p FROM Payment p
            WHERE (:memberId IS NULL OR p.member.id = :memberId)
            AND (:weekNumber IS NULL OR p.weekNumber = :weekNumber)
            AND (:paymentYear IS NULL OR p.paymentYear = :paymentYear)
            AND (:method IS NULL OR p.paymentMethod = :method)
            AND (:startDate IS NULL OR p.paymentDate >= :startDate)
            AND (:endDate IS NULL OR p.paymentDate <= :endDate)
            ORDER BY p.paymentDate DESC, p.id DESC
            """)
    Page<Payment> filter(
            @Param("memberId") Long memberId,
            @Param("weekNumber") Integer weekNumber,
            @Param("paymentYear") Integer paymentYear,
            @Param("method") PaymentMethod method,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            Pageable pageable
    );

    @Query("""
            SELECT p FROM Payment p
            WHERE (:memberId IS NULL OR p.member.id = :memberId)
            AND (:method IS NULL OR p.paymentMethod = :method)
            AND p.paymentDate BETWEEN :startDate AND :endDate
            ORDER BY p.paymentDate ASC
            """)
    List<Payment> findForReport(
            @Param("memberId") Long memberId,
            @Param("method") PaymentMethod method,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("""
            SELECT COALESCE(SUM(p.amountPaid), 0)
            FROM Payment p
            WHERE p.weekNumber = :week
            AND p.paymentYear = :year
            """)
    BigDecimal sumAmountByWeek(
            @Param("week") Integer week,
            @Param("year") Integer year
    );

    @Query("""
            SELECT COALESCE(SUM(p.amountPaid), 0)
            FROM Payment p
            WHERE p.weekNumber = :week
            AND p.paymentYear = :year
            AND p.paymentMethod = :method
            """)
    BigDecimal sumAmountByWeekAndMethod(
            @Param("week") Integer week,
            @Param("year") Integer year,
            @Param("method") PaymentMethod method
    );

    @Query("""
            SELECT COALESCE(SUM(p.remainingAmount), 0)
            FROM Payment p
            WHERE p.weekNumber = :week
            AND p.paymentYear = :year
            """)
    BigDecimal sumRemainingByWeek(
            @Param("week") Integer week,
            @Param("year") Integer year
    );

    @Query("""
            SELECT COALESCE(SUM(p.amountPaid), 0)
            FROM Payment p
            WHERE p.paymentMethod = :method
            """)
    BigDecimal sumAllByMethod(
            @Param("method") PaymentMethod method
    );

    /**
     * Every payment ever recorded, across all weeks/members.
     */
    @Query("""
            SELECT COALESCE(SUM(p.amountPaid), 0)
            FROM Payment p
            """)
    BigDecimal sumAllAmountPaid();

    /**
     * Every payment's remaining shortfall, across all weeks/members.
     */
    @Query("""
            SELECT COALESCE(SUM(p.remainingAmount), 0)
            FROM Payment p
            """)
    BigDecimal sumAllRemainingAmount();

    /**
     * Sum of remainingAmount across every payment ever recorded
     * for this member.
     */
    @Query("""
            SELECT COALESCE(SUM(p.remainingAmount), 0)
            FROM Payment p
            WHERE p.member.id = :memberId
            """)
    BigDecimal sumRemainingAmountByMemberId(
            @Param("memberId") Long memberId
    );

    /**
     * Sum of amountPaid across every payment ever recorded
     * for this member.
     */
    @Query("""
            SELECT COALESCE(SUM(p.amountPaid), 0)
            FROM Payment p
            WHERE p.member.id = :memberId
            """)
    BigDecimal sumAmountPaidByMemberId(
            @Param("memberId") Long memberId
    );

        /** Per staff: [staffId, total collected, pending shortfall on non-closed loans]. */
    @Query("""
            SELECT m.staffMember.id,
                   COALESCE(SUM(p.amountPaid), 0),
                   COALESCE(SUM(CASE WHEN m.status <> :closed THEN p.remainingAmount ELSE 0 END), 0)
            FROM Payment p JOIN p.member m
            WHERE m.staffMember IS NOT NULL
            GROUP BY m.staffMember.id
            """)
    List<Object[]> collectionByStaff(@Param("closed") MemberStatus closed);
        /** [memberId, total amount paid] for every member that has payments. */
    @Query("""
            SELECT p.member.id, COALESCE(SUM(p.amountPaid), 0)
            FROM Payment p
            GROUP BY p.member.id
            """)
    List<Object[]> totalPaidByMember();

}