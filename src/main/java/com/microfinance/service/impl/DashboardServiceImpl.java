package com.microfinance.service.impl;

import com.microfinance.dto.response.DashboardChartsResponse;
import com.microfinance.dto.response.DashboardSummaryResponse;
import com.microfinance.dto.response.GroupSummaryResponse;
import com.microfinance.dto.response.InsuranceProcessingSummaryResponse;
import com.microfinance.entity.Member;
import com.microfinance.entity.Payment;
import com.microfinance.entity.WeeklyCollection;
import com.microfinance.entity.enums.MemberStatus;
import com.microfinance.entity.enums.PaymentMethod;
import com.microfinance.repository.MemberRepository;
import com.microfinance.repository.PaymentRepository;
import com.microfinance.repository.SavingsPaymentRepository;
import com.microfinance.repository.WeeklyCollectionRepository;
import com.microfinance.service.DashboardService;
import com.microfinance.util.DateUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final MemberRepository memberRepository;
    private final PaymentRepository paymentRepository;
    private final WeeklyCollectionRepository weeklyCollectionRepository;
    private final SavingsPaymentRepository savingsPaymentRepository;

    @Override
    public DashboardSummaryResponse getSummary() {

        int week = DateUtil.currentIsoWeek();
        int year = DateUtil.currentIsoWeekYear();

        BigDecimal totalThisWeek =
                paymentRepository.sumAmountByWeek(week, year);

        BigDecimal cash =
                paymentRepository.sumAmountByWeekAndMethod(
                        week,
                        year,
                        PaymentMethod.CASH
                );

        BigDecimal online =
                paymentRepository.sumAmountByWeekAndMethod(
                        week,
                        year,
                        PaymentMethod.ONLINE
                );

        BigDecimal remainingDue =
                paymentRepository.sumRemainingByWeek(
                        week,
                        year
                );

        BigDecimal expectedThisWeek =
                memberRepository.sumWeeklyAmountByStatus(
                        MemberStatus.ACTIVE
                );

        BigDecimal totalOutstanding =
                expectedThisWeek
                        .subtract(totalThisWeek)
                        .max(BigDecimal.ZERO);

        BigDecimal totalCollectionAllTime =
                paymentRepository.sumAllAmountPaid()
                        // Monthly Savings deposits are collected money too —
                        // fold them into the same "Total Collection" figure
                        // shown on the Dashboard/Admin pages, not just loan
                        // repayments.
                        .add(savingsPaymentRepository.sumAllAmountPaid());

        BigDecimal totalOutstandingAllTime =
                paymentRepository.sumAllRemainingAmount();

        return DashboardSummaryResponse.builder()
                .totalMembers(memberRepository.count())
                .activeMembers(
                        memberRepository.countByStatus(
                                MemberStatus.ACTIVE
                        )
                )
                .currentWeekNumber(week)
                .currentYear(year)
                .totalCollectionThisWeek(totalThisWeek)
                .cashCollection(cash)
                .onlineCollection(online)
                .remainingDue(remainingDue)
                .totalOutstanding(totalOutstanding)
                .totalCollectionAllTime(totalCollectionAllTime)
                .totalOutstandingAllTime(totalOutstandingAllTime)
                .build();
    }

    @Override
    public DashboardChartsResponse getCharts() {

        List<WeeklyCollection> lastWeeks =
                weeklyCollectionRepository
                        .findTop8ByOrderByCollectionYearDescWeekNumberDesc();

        lastWeeks.sort(
                Comparator.comparing(
                                WeeklyCollection::getCollectionYear
                        )
                        .thenComparing(
                                WeeklyCollection::getWeekNumber
                        )
        );

        List<String> weeklyLabels =
                new ArrayList<>();

        List<BigDecimal> weeklyCollection =
                new ArrayList<>();

        for (WeeklyCollection wc : lastWeeks) {

            weeklyLabels.add(
                    "W" + wc.getWeekNumber()
            );

            weeklyCollection.add(
                    wc.getTotalCollection()
            );
        }

        List<String> monthlyLabels =
                new ArrayList<>();

        List<BigDecimal> monthlyCollection =
                new ArrayList<>();

        LocalDate cursor =
                LocalDate.now()
                        .withDayOfMonth(1)
                        .minusMonths(5);

        for (int i = 0; i < 6; i++) {

            LocalDate start =
                    cursor.withDayOfMonth(1);

            LocalDate end =
                    DateUtil.lastDayOfMonth(
                            start.getYear(),
                            start.getMonthValue()
                    );

            List<Payment> monthPayments =
                    paymentRepository.findForReport(
                            null,
                            null,
                            start,
                            end
                    );

            BigDecimal sum =
                    monthPayments.stream()
                            .map(Payment::getAmountPaid)
                            .reduce(
                                    BigDecimal.ZERO,
                                    BigDecimal::add
                            );

            monthlyLabels.add(
                    start.getMonth()
                            .getDisplayName(
                                    TextStyle.SHORT,
                                    Locale.ENGLISH
                            )
                            + " "
                            + start.getYear()
            );

            monthlyCollection.add(sum);

            cursor = cursor.plusMonths(1);
        }

        BigDecimal cashTotal =
                paymentRepository.sumAllByMethod(
                        PaymentMethod.CASH
                );

        BigDecimal onlineTotal =
                paymentRepository.sumAllByMethod(
                        PaymentMethod.ONLINE
                );

        int week =
                DateUtil.currentIsoWeek();

        int year =
                DateUtil.currentIsoWeekYear();

        List<Member> topDue =
                memberRepository.findAll()
                        .stream()
                        .filter(
                                m ->
                                        m.getStatus()
                                                == MemberStatus.ACTIVE
                        )
                        .map(
                                m ->
                                        Map.entry(
                                                m,
                                                currentWeekRemaining(
                                                        m,
                                                        week,
                                                        year
                                                )
                                        )
                        )
                        .filter(
                                e ->
                                        e.getValue()
                                                .compareTo(
                                                        BigDecimal.ZERO
                                                ) > 0
                        )
                        .sorted(
                                Comparator.comparing(
                                                (
                                                        Map.Entry<Member, BigDecimal> e
                                                ) ->
                                                        e.getValue()
                                        )
                                        .reversed()
                        )
                        .limit(5)
                        .map(Map.Entry::getKey)
                        .toList();

        List<String> remainingMemberLabels =
                new ArrayList<>();

        List<BigDecimal> remainingAmounts =
                new ArrayList<>();

        for (Member m : topDue) {

            remainingMemberLabels.add(
                    m.getName()
            );

            remainingAmounts.add(
                    currentWeekRemaining(
                            m,
                            week,
                            year
                    )
            );
        }

        return DashboardChartsResponse.builder()
                .weeklyLabels(weeklyLabels)
                .weeklyCollection(weeklyCollection)
                .monthlyLabels(monthlyLabels)
                .monthlyCollection(monthlyCollection)
                .cashTotal(cashTotal)
                .onlineTotal(onlineTotal)
                .remainingMemberLabels(
                        remainingMemberLabels
                )
                .remainingAmounts(
                        remainingAmounts
                )
                .build();
    }

    private BigDecimal currentWeekRemaining(
            Member member,
            int week,
            int year
    ) {

        Optional<Payment> payment =
                paymentRepository
                        .findByMember_IdAndWeekNumberAndPaymentYear(
                                member.getId(),
                                week,
                                year
                        );

        return payment
                .map(Payment::getRemainingAmount)
                .orElse(member.getWeeklyAmount());
    }

    @Override
    public List<GroupSummaryResponse> getGroupSummaries() {

        int week =
                DateUtil.currentIsoWeek();

        int year =
                DateUtil.currentIsoWeekYear();

        List<Member> heads =
                memberRepository
                        .findByHeadMemberTrueOrderByNameAsc();

        List<GroupSummaryResponse> result =
                new ArrayList<>();

        for (Member head : heads) {

            List<Member> subMembers =
                    memberRepository
                            .findByParentHead_IdOrderByNameAsc(
                                    head.getId()
                            );

            List<Member> group =
                    new ArrayList<>();

            group.add(head);
            group.addAll(subMembers);

            BigDecimal expected =
                    BigDecimal.ZERO;

            BigDecimal collected =
                    BigDecimal.ZERO;

            for (Member m : group) {

                if (m.getWeeklyAmount() != null) {

                    expected =
                            expected.add(
                                    m.getWeeklyAmount()
                            );
                }

                Optional<Payment> payment =
                        paymentRepository
                                .findByMember_IdAndWeekNumberAndPaymentYear(
                                        m.getId(),
                                        week,
                                        year
                                );

                collected =
                        collected.add(
                                payment
                                        .map(
                                                Payment::getAmountPaid
                                        )
                                        .orElse(
                                                BigDecimal.ZERO
                                        )
                        );
            }

            /*
             * Calculate how many complete weekly collections
             * have been finished for this group.
             *
             * A week is counted only when EVERY member in the
             * group has a completed payment for that week.
             */
            int billGenerated =
                    countCompletedWeeks(group);

            result.add(
                    GroupSummaryResponse.builder()
                            .headId(head.getId())
                            .headMemberCode(
                                    head.getMemberCode()
                            )
                            .headName(
                                    head.getName()
                            )
                            .centerPlace(
                                    head.getCenterPlace()
                            )
                            .groupId(
                                    head.getGroupId()
                             )
                            .groupName(head.getGroupName()
                            )
                            .totalMembers(
                                    group.size()
                            )
                            .totalCollectedThisWeek(
                                    collected
                            )
                            .totalExpectedThisWeek(
                                    expected
                            )
                            .remainingThisWeek(
                                    expected
                                            .subtract(
                                                    collected
                                            )
                                            .max(
                                                    BigDecimal.ZERO
                                            )
                            )
                            .billGenerated(
                                    billGenerated
                            )
                            .build()
            );
        }

        return result;
    }

    /**
     * Calculates the number of fully completed weeks for a group.
     *
     * Example:
     *
     * Group has 4 members.
     *
     * Week 10:
     *   Member 1 -> Paid
     *   Member 2 -> Paid
     *   Member 3 -> Paid
     *   Member 4 -> Paid
     *
     * Week 10 is complete -> count = 1
     *
     * Week 11:
     *   Member 1 -> Paid
     *   Member 2 -> Paid
     *   Member 3 -> Paid
     *   Member 4 -> Partial
     *
     * Week 11 is NOT complete -> count remains 1
     *
     * When Member 4 is updated to full payment:
     *
     * Week 11 -> Complete
     * count becomes 2.
     */
    private int countCompletedWeeks(
            List<Member> group
    ) {

        if (group == null || group.isEmpty()) {
            return 0;
        }

        List<Long> memberIds =
                group.stream()
                        .map(Member::getId)
                        .filter(id -> id != null)
                        .toList();

        if (memberIds.isEmpty()) {
            return 0;
        }

        /*
         * Load all payment records belonging to this group.
         */
        List<Payment> payments =
                paymentRepository.findByMember_IdIn(
                        memberIds
                );

        /*
         * Map:
         *
         * "2026-10" -> [memberId1, memberId2, memberId3]
         *
         * The set contains members who completely paid
         * that particular week.
         */
        Map<String, Set<Long>>
                fullyPaidMembersByWeek =
                new HashMap<>();

        for (Payment payment : payments) {

            if (payment.getMember() == null
                    || payment.getMember().getId() == null) {
                continue;
            }

            if (payment.getWeekNumber() == null
                    || payment.getPaymentYear() == null) {
                continue;
            }

            /*
             * PaymentServiceImpl sets status to PAID when
             * remainingAmount reaches zero.
             *
             * We also check remainingAmount directly so that
             * existing/older records are handled correctly
             * even if their status is inconsistent.
             */
            BigDecimal remaining =
                    payment.getRemainingAmount();

            boolean fullyPaid =
                    remaining != null
                            && remaining.compareTo(
                                    BigDecimal.ZERO
                            ) <= 0;

            if (!fullyPaid) {
                continue;
            }

            String weekKey =
                    payment.getPaymentYear()
                            + "-"
                            + payment.getWeekNumber();

            fullyPaidMembersByWeek
                    .computeIfAbsent(
                            weekKey,
                            key ->
                                    new HashSet<>()
                    )
                    .add(
                            payment.getMember().getId()
                    );
        }

        /*
         * All members that belong to the current group.
         */
        Set<Long> groupMemberIds =
                new HashSet<>(
                        memberIds
                );

        /*
         * A week counts as one generated bill only when
         * every member of this group has fully paid that week.
         */
        int completedWeeks = 0;

        for (Set<Long> fullyPaidMemberIds :
                fullyPaidMembersByWeek.values()) {

            if (fullyPaidMemberIds.containsAll(
                    groupMemberIds
            )) {
                completedWeeks++;
            }
        }

        return completedWeeks;
    }

    @Override
    public InsuranceProcessingSummaryResponse
    getInsuranceProcessingSummary() {

        long membersPaidCount =
                memberRepository
                        .countMembersWithInsuranceOrProcessingPaid();

        BigDecimal totalInsurance =
                memberRepository.sumInsuranceAmount();

        BigDecimal totalProcessing =
                memberRepository.sumProcessingAmount();

        return InsuranceProcessingSummaryResponse.builder()
                .membersPaidCount(
                        membersPaidCount
                )
                .totalInsuranceCollected(
                        totalInsurance
                )
                .totalProcessingCollected(
                        totalProcessing
                )
                .totalCollected(
                        totalInsurance.add(
                                totalProcessing
                        )
                )
                .build();
    }
}