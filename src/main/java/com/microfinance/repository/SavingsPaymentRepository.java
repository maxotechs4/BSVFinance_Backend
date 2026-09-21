package com.microfinance.repository;

import com.microfinance.entity.SavingsPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface SavingsPaymentRepository extends JpaRepository<SavingsPayment, Long> {

    List<SavingsPayment> findBySavingsMember_IdOrderByInstallmentNumberDesc(Long savingsMemberId);

    Optional<SavingsPayment> findBySavingsMember_IdAndInstallmentNumber(Long savingsMemberId, Integer installmentNumber);

    @Query("SELECT COALESCE(SUM(p.amountPaid), 0) FROM SavingsPayment p WHERE p.savingsMember.id = :savingsMemberId")
    BigDecimal sumAmountPaidBySavingsMemberId(@Param("savingsMemberId") Long savingsMemberId);

    /** Sum of amountPaid across every savings payment ever recorded — folded into "Total Collection" alongside loan payments. */
    @Query("SELECT COALESCE(SUM(p.amountPaid), 0) FROM SavingsPayment p")
    BigDecimal sumAllAmountPaid();

    long countBySavingsMember_IdAndStatus(Long savingsMemberId, com.microfinance.entity.enums.PaymentStatus status);

    long count();
}
