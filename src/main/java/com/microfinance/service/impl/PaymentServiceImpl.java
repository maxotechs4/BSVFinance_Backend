package com.microfinance.service.impl;

import com.microfinance.dto.request.CollectionRequest;
import com.microfinance.dto.request.PaymentRequest;
import com.microfinance.dto.response.PageResponse;
import com.microfinance.dto.response.PaymentResponse;
import com.microfinance.entity.Member;
import com.microfinance.entity.Payment;
import com.microfinance.entity.WeeklyCollection;
import com.microfinance.entity.enums.PaymentMethod;
import com.microfinance.entity.enums.PaymentStatus;
import com.microfinance.exception.BadRequestException;
import com.microfinance.exception.DuplicateResourceException;
import com.microfinance.exception.ResourceNotFoundException;
import com.microfinance.mapper.PaymentMapper;
import com.microfinance.repository.MemberRepository;
import com.microfinance.repository.PaymentRepository;
import com.microfinance.repository.WeeklyCollectionRepository;
import com.microfinance.service.PaymentService;
import com.microfinance.util.DateUtil;
import com.microfinance.util.OutstandingCalculator;
import com.microfinance.util.ReceiptNumberGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.IsoFields;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private static final int MAX_RECEIPT_RETRIES = 5;

    private final PaymentRepository paymentRepository;
    private final MemberRepository memberRepository;
    private final WeeklyCollectionRepository weeklyCollectionRepository;
    private final PaymentMapper paymentMapper;
    private final ReceiptNumberGenerator receiptNumberGenerator;


    // ============================================================
    // ADD PAYMENT
    // ============================================================

    @Override
    @Transactional
    public PaymentResponse addPayment(PaymentRequest request) {

        validateOnlineDetails(request);

        Member member = memberRepository.findById(request.getMemberId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Member not found with id: " + request.getMemberId()
                        )
                );


        /*
         * Prevent duplicate payment for the same member,
         * week and year.
         */
        paymentRepository.findByMember_IdAndWeekNumberAndPaymentYear(
                        member.getId(),
                        request.getWeekNumber(),
                        request.getPaymentYear()
                )
                .ifPresent(p -> {
                    throw new DuplicateResourceException(
                            "A payment for week "
                                    + request.getWeekNumber()
                                    + "/"
                                    + request.getPaymentYear()
                                    + " already exists for this member. "
                                    + "Edit the existing payment instead."
                    );
                });


        BigDecimal weeklyAmount =
                member.getWeeklyAmount();

        BigDecimal amountPaid =
                request.getAmountPaid();


        BigDecimal remaining =
                weeklyAmount
                        .subtract(amountPaid)
                        .max(BigDecimal.ZERO);


        BigDecimal extra =
                amountPaid
                        .subtract(weeklyAmount)
                        .max(BigDecimal.ZERO);


        PaymentStatus status =
                remaining.compareTo(BigDecimal.ZERO) == 0
                        ? PaymentStatus.PAID
                        : PaymentStatus.PARTIAL;


        Payment payment = Payment.builder()
                .member(member)
                .weekNumber(request.getWeekNumber())
                .paymentYear(request.getPaymentYear())
                .weeklyAmount(weeklyAmount)
                .amountPaid(amountPaid)
                .remainingAmount(remaining)
                .extraAmount(extra)
                .status(status)
                .paymentMethod(request.getPaymentMethod())
                .upiTransactionId(
                        request.getPaymentMethod() == PaymentMethod.ONLINE
                                ? request.getUpiTransactionId()
                                : null
                )
                .paymentDate(request.getPaymentDate())
                .remarks(request.getRemarks())
                .build();


        Payment saved =
                saveWithGeneratedReceipt(payment);


        /*
         * Add any extra amount to the member's
         * credit balance.
         */
        member.setCreditBalance(
                member.getCreditBalance().add(extra)
        );

        recalculateOutstanding(member);

        memberRepository.save(member);


        /*
         * Recalculate weekly collection totals.
         */
        recalcWeeklyCollection(
                request.getWeekNumber(),
                request.getPaymentYear()
        );


        log.info(
                "Recorded payment {} for member {} (week {}/{})",
                saved.getReceiptNumber(),
                member.getMemberCode(),
                request.getWeekNumber(),
                request.getPaymentYear()
        );


        return paymentMapper.toResponse(saved);
    }


    // ============================================================
    // COLLECTION PAGE PAYMENT
    //
    // IMPORTANT:
    // The old code calculated:
    //
    // last payment week + 1
    //
    // That caused:
    //
    // W36 payment saved
    //       ↓
    // immediately target W37
    //
    // NEW BEHAVIOUR:
    //
    // The week is calculated from the ACTUAL
    // payment date supplied by the Collection page.
    //
    // Example:
    //
    // 31 Aug 2026 -> W36
    // 1 Sep 2026  -> W36
    // 6 Sep 2026  -> W36
    // 7 Sep 2026  -> W37
    // ============================================================

    @Override
    @Transactional
    public PaymentResponse collectNextWeekPayment(
            CollectionRequest request
    ) {

        /*
         * Validate online payment information.
         */
        if (request.getPaymentMethod() == PaymentMethod.ONLINE
                &&
                (
                        request.getUpiTransactionId() == null
                                ||
                        request.getUpiTransactionId().isBlank()
                )
        ) {
            throw new BadRequestException(
                    "UPI transaction ID is required when payment method is ONLINE"
            );
        }


        /*
         * Find member.
         */
        Member member =
                memberRepository.findById(
                        request.getMemberId()
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Member not found with id: "
                                        + request.getMemberId()
                        )
                );


        /*
         * ========================================================
         * IMPORTANT WEEK CALCULATION
         * ========================================================
         *
         * The week is now determined from the PAYMENT DATE.
         *
         * We do NOT look at the member's previous payment.
         *
         * We do NOT do:
         *
         * lastWeek + 1
         *
         * This prevents the week from advancing immediately
         * after a payment is entered.
         */

        LocalDate paymentDate =
                request.getPaymentDate();


        if (paymentDate == null) {
            throw new BadRequestException(
                    "Payment date is required"
            );
        }


        int paymentWeek =
                paymentDate.get(
                        IsoFields.WEEK_OF_WEEK_BASED_YEAR
                );


        int paymentYear =
                paymentDate.get(
                        IsoFields.WEEK_BASED_YEAR
                );


        /*
         * Calculate payment amounts.
         */
        BigDecimal weeklyAmount =
                member.getWeeklyAmount();

        BigDecimal amountPaid =
                request.getAmountPaid();


        BigDecimal remaining =
                weeklyAmount
                        .subtract(amountPaid)
                        .max(BigDecimal.ZERO);


        BigDecimal extra =
                amountPaid
                        .subtract(weeklyAmount)
                        .max(BigDecimal.ZERO);


        PaymentStatus status =
                remaining.compareTo(BigDecimal.ZERO) == 0
                        ? PaymentStatus.PAID
                        : PaymentStatus.PARTIAL;


        /*
         * ========================================================
         * CHECK WHETHER PAYMENT ALREADY EXISTS
         * ========================================================
         *
         * If this member already has a payment for the same
         * calendar week, update that payment.
         *
         * Otherwise create a new payment.
         */

        Optional<Payment> existing =
                paymentRepository
                        .findByMember_IdAndWeekNumberAndPaymentYear(
                                member.getId(),
                                paymentWeek,
                                paymentYear
                        );


        Payment saved;


        if (existing.isPresent()) {

            /*
             * ----------------------------------------------------
             * UPDATE EXISTING PAYMENT
             * ----------------------------------------------------
             */

            Payment payment =
                    existing.get();


            BigDecimal previousExtra =
                    payment.getExtraAmount();


            payment.setWeeklyAmount(
                    weeklyAmount
            );

            payment.setAmountPaid(
                    amountPaid
            );

            payment.setRemainingAmount(
                    remaining
            );

            payment.setExtraAmount(
                    extra
            );

            payment.setStatus(
                    status
            );

            payment.setPaymentMethod(
                    request.getPaymentMethod()
            );

            payment.setUpiTransactionId(
                    request.getPaymentMethod()
                            == PaymentMethod.ONLINE
                            ? request.getUpiTransactionId()
                            : null
            );

            payment.setPaymentDate(
                    request.getPaymentDate()
            );

            payment.setRemarks(
                    request.getRemarks()
            );


            saved =
                    paymentRepository.save(
                            payment
                    );

            // Ensure the update above is visible to the payment-history
            // query that recalculateOutstanding() runs below.
            paymentRepository.flush();


            /*
             * Recalculate credit balance.
             *
             * Remove the old extra amount and
             * add the new extra amount.
             */
            BigDecimal adjustedBalance =
                    member.getCreditBalance()
                            .subtract(previousExtra)
                            .add(extra);


            member.setCreditBalance(
                    adjustedBalance.max(
                            BigDecimal.ZERO
                    )
            );

        } else {

            /*
             * ----------------------------------------------------
             * CREATE NEW PAYMENT
             * ----------------------------------------------------
             */

            Payment payment =
                    Payment.builder()
                            .member(member)

                            /*
                             * IMPORTANT:
                             * Use the week calculated from
                             * the payment date.
                             */
                            .weekNumber(
                                    paymentWeek
                            )

                            .paymentYear(
                                    paymentYear
                            )

                            .weeklyAmount(
                                    weeklyAmount
                            )

                            .amountPaid(
                                    amountPaid
                            )

                            .remainingAmount(
                                    remaining
                            )

                            .extraAmount(
                                    extra
                            )

                            .status(
                                    status
                            )

                            .paymentMethod(
                                    request.getPaymentMethod()
                            )

                            .upiTransactionId(
                                    request.getPaymentMethod()
                                            == PaymentMethod.ONLINE
                                            ? request.getUpiTransactionId()
                                            : null
                            )

                            .paymentDate(
                                    request.getPaymentDate()
                            )

                            .remarks(
                                    request.getRemarks()
                            )

                            .build();


            /*
             * Generate receipt number and save.
             */
            saved =
                    saveWithGeneratedReceipt(
                            payment
                    );


            /*
             * Add extra payment amount to
             * member's credit balance.
             */
            member.setCreditBalance(
                    member.getCreditBalance()
                            .add(extra)
            );
        }


        /*
         * Recalculate the outstanding loan amount now
         * that this week's payment has been saved.
         */
        recalculateOutstanding(member);


        /*
         * Save updated member credit balance.
         */
        memberRepository.save(member);


        /*
         * Recalculate weekly collection for the
         * actual payment week.
         */
        recalcWeeklyCollection(
                paymentWeek,
                paymentYear
        );


        log.info(
                "Collected payment for member {} -> week {}/{} (amount {})",
                member.getMemberCode(),
                paymentWeek,
                paymentYear,
                amountPaid
        );


        return paymentMapper.toResponse(
                saved
        );
    }


    // ============================================================
    // GENERATE RECEIPT NUMBER
    // ============================================================

    private Payment saveWithGeneratedReceipt(
            Payment payment
    ) {

        for (
                int attempt = 0;
                attempt < MAX_RECEIPT_RETRIES;
                attempt++
        ) {

            payment.setReceiptNumber(
                    receiptNumberGenerator.generate(
                            attempt
                    )
            );


            try {

                return paymentRepository.saveAndFlush(
                        payment
                );

            } catch (
                    DataIntegrityViolationException e
            ) {

                log.warn(
                        "Receipt number collision on attempt {}, retrying",
                        attempt
                );
            }
        }


        throw new IllegalStateException(
                "Could not generate a unique receipt number after "
                        + MAX_RECEIPT_RETRIES
                        + " attempts"
        );
    }


    // ============================================================
    // UPDATE PAYMENT
    // ============================================================

    @Override
    @Transactional
    public PaymentResponse updatePayment(
            Long id,
            PaymentRequest request
    ) {

        validateOnlineDetails(request);


        Payment payment =
                paymentRepository.findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Payment not found with id: "
                                                + id
                                )
                        );


        Member member =
                payment.getMember();


        BigDecimal previousExtra =
                payment.getExtraAmount();


        int previousWeek =
                payment.getWeekNumber();


        int previousYear =
                payment.getPaymentYear();


        /*
         * If week/year changed, make sure another
         * payment does not already exist for that
         * member and period.
         */
        boolean periodChanged =
                previousWeek != request.getWeekNumber()
                        ||
                previousYear != request.getPaymentYear();


        if (periodChanged) {

            paymentRepository
                    .findByMember_IdAndWeekNumberAndPaymentYear(
                            member.getId(),
                            request.getWeekNumber(),
                            request.getPaymentYear()
                    )
                    .filter(
                            existing ->
                                    !existing
                                            .getId()
                                            .equals(id)
                    )
                    .ifPresent(
                            existing -> {
                                throw new DuplicateResourceException(
                                        "A payment for week "
                                                + request.getWeekNumber()
                                                + "/"
                                                + request.getPaymentYear()
                                                + " already exists for this member. "
                                                + "Edit that entry instead."
                                );
                            }
                    );
        }


        BigDecimal weeklyAmount =
                member.getWeeklyAmount();


        BigDecimal amountPaid =
                request.getAmountPaid();


        BigDecimal remaining =
                weeklyAmount
                        .subtract(amountPaid)
                        .max(BigDecimal.ZERO);


        BigDecimal extra =
                amountPaid
                        .subtract(weeklyAmount)
                        .max(BigDecimal.ZERO);


        PaymentStatus status =
                remaining.compareTo(BigDecimal.ZERO) == 0
                        ? PaymentStatus.PAID
                        : PaymentStatus.PARTIAL;


        payment.setWeekNumber(
                request.getWeekNumber()
        );


        payment.setPaymentYear(
                request.getPaymentYear()
        );


        payment.setWeeklyAmount(
                weeklyAmount
        );


        payment.setAmountPaid(
                amountPaid
        );


        payment.setRemainingAmount(
                remaining
        );


        payment.setExtraAmount(
                extra
        );


        payment.setStatus(
                status
        );


        payment.setPaymentMethod(
                request.getPaymentMethod()
        );


        payment.setUpiTransactionId(
                request.getPaymentMethod()
                        == PaymentMethod.ONLINE
                        ? request.getUpiTransactionId()
                        : null
        );


        payment.setPaymentDate(
                request.getPaymentDate()
        );


        payment.setRemarks(
                request.getRemarks()
        );


        Payment saved =
                paymentRepository.save(
                        payment
                );

        // Ensure the update above is visible to the payment-history
        // query that recalculateOutstanding() runs below.
        paymentRepository.flush();


        /*
         * Reconcile credit balance.
         */
        BigDecimal adjustedBalance =
                member.getCreditBalance()
                        .subtract(previousExtra)
                        .add(extra);


        member.setCreditBalance(
                adjustedBalance.max(
                        BigDecimal.ZERO
                )
        );

        recalculateOutstanding(member);


        memberRepository.save(
                member
        );


        /*
         * Recalculate the previous week.
         */
        recalcWeeklyCollection(
                previousWeek,
                previousYear
        );


        /*
         * If the payment moved to another
         * week/year, recalculate the new week too.
         */
        if (periodChanged) {

            recalcWeeklyCollection(
                    request.getWeekNumber(),
                    request.getPaymentYear()
            );
        }


        return paymentMapper.toResponse(
                saved
        );
    }


    // ============================================================
    // DELETE PAYMENT
    // ============================================================

    @Override
    @Transactional
    public void deletePayment(
            Long id
    ) {

        Payment payment =
                paymentRepository.findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Payment not found with id: "
                                                + id
                                )
                        );


        Member member =
                payment.getMember();


        /*
         * Remove the extra amount from the
         * member's credit balance.
         */
        member.setCreditBalance(
                member.getCreditBalance()
                        .subtract(
                                payment.getExtraAmount()
                        )
                        .max(
                                BigDecimal.ZERO
                        )
        );


        int week =
                payment.getWeekNumber();


        int year =
                payment.getPaymentYear();


        /*
         * Delete payment, then flush so the outstanding-amount
         * recalculation below queries the payment history
         * with this payment already gone.
         */
        paymentRepository.delete(
                payment
        );

        paymentRepository.flush();

        recalculateOutstanding(member);


        memberRepository.save(
                member
        );


        /*
         * Recalculate weekly collection.
         */
        recalcWeeklyCollection(
                week,
                year
        );


        log.info(
                "Deleted payment id={} (week {}/{})",
                id,
                week,
                year
        );
    }


    // ============================================================
    // GET PAYMENT BY ID
    // ============================================================

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentById(
            Long id
    ) {

        Payment payment =
                paymentRepository.findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Payment not found with id: "
                                                + id
                                )
                        );


        return paymentMapper.toResponse(
                payment
        );
    }


    // ============================================================
    // GET PAYMENTS BY MEMBER
    // ============================================================

    @Override
    @Transactional(readOnly = true)
    public List<PaymentResponse> getPaymentsByMember(
            Long memberId
    ) {

        return paymentRepository
                .findByMember_IdOrderByPaymentYearDescWeekNumberDesc(
                        memberId
                )
                .stream()
                .map(paymentMapper::toResponse)
                .toList();
    }


    // ============================================================
    // GET PAYMENTS WITH FILTERS
    // ============================================================

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PaymentResponse> getPayments(
            Long memberId,
            Integer weekNumber,
            Integer paymentYear,
            PaymentMethod method,
            LocalDate startDate,
            LocalDate endDate,
            int page,
            int size
    ) {

        Pageable pageable =
                PageRequest.of(
                        page,
                        size,
                        Sort.by(
                                Sort.Direction.DESC,
                                "paymentDate"
                        )
                );


        var result =
                paymentRepository.filter(
                        memberId,
                        weekNumber,
                        paymentYear,
                        method,
                        startDate,
                        endDate,
                        pageable
                );


        return PageResponse.of(
                result.map(
                        paymentMapper::toResponse
                )
        );
    }


    // ============================================================
    // VALIDATE ONLINE PAYMENT
    // ============================================================

    private void validateOnlineDetails(
            PaymentRequest request
    ) {

        if (
                request.getPaymentMethod()
                        == PaymentMethod.ONLINE
                        &&
                (
                        request.getUpiTransactionId() == null
                                ||
                        request.getUpiTransactionId().isBlank()
                )
        ) {

            throw new BadRequestException(
                    "UPI transaction ID is required when payment method is ONLINE"
            );
        }
    }


    // ============================================================
    // RECALCULATE WEEKLY COLLECTION
    // ============================================================

    private void recalcWeeklyCollection(
            int weekNumber,
            int year
    ) {

        BigDecimal total =
                paymentRepository.sumAmountByWeek(
                        weekNumber,
                        year
                );


        BigDecimal cash =
                paymentRepository.sumAmountByWeekAndMethod(
                        weekNumber,
                        year,
                        PaymentMethod.CASH
                );


        BigDecimal online =
                paymentRepository.sumAmountByWeekAndMethod(
                        weekNumber,
                        year,
                        PaymentMethod.ONLINE
                );


        BigDecimal due =
                paymentRepository.sumRemainingByWeek(
                        weekNumber,
                        year
                );


        long membersPaid =
                paymentRepository
                        .countByWeekNumberAndPaymentYear(
                                weekNumber,
                                year
                        );


        WeeklyCollection collection =
                weeklyCollectionRepository
                        .findByWeekNumberAndCollectionYear(
                                weekNumber,
                                year
                        )
                        .orElseGet(
                                () ->
                                        WeeklyCollection
                                                .builder()
                                                .weekNumber(
                                                        weekNumber
                                                )
                                                .collectionYear(
                                                        year
                                                )
                                                .build()
                        );


        collection.setTotalCollection(
                total
        );


        collection.setCashCollection(
                cash
        );


        collection.setOnlineCollection(
                online
        );


        collection.setTotalDue(
                due
        );


        collection.setMembersPaid(
                (int) membersPaid
        );


        weeklyCollectionRepository.save(
                collection
        );
    }


    // ============================================================
    // RECALCULATE OUTSTANDING LOAN AMOUNT
    // ============================================================
    //
    // Every weekly/monthly collection is split into a fixed Interest portion
    // (based on the loan amount and interest rate — not the amount actually
    // paid) plus a Principal portion. Outstanding Amount counts down from
    // the total amount expected to be collected for this loan
    // (weeklyAmount * totalWeeks) rather than the raw loan amount.
    // Recomputing from the member's full payment history (rather than
    // nudging a running total) keeps the figure correct even after a
    // payment is edited or deleted later.
    // ============================================================

    private void recalculateOutstanding(Member member) {
        List<Payment> payments = paymentRepository.findByMember_IdOrderByPaymentYearDescWeekNumberDesc(member.getId());
        member.setOutstandingAmount(
                OutstandingCalculator.calculate(
                        member.getWeeklyAmount(),
                        member.getTotalWeeks(),
                        member.getLoanAmount(),
                        member.getInterestPercentage(),
                        payments
                )
        );
        member.setOutstandingAmountWithInterest(
                OutstandingCalculator.calculateWithInterest(
                        member.getWeeklyAmount(),
                        member.getTotalWeeks(),
                        member.getLoanAmount(),
                        member.getInterestPercentage(),
                        payments
                )
        );
    }
}