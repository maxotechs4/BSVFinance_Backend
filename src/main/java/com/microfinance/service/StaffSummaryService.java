package com.microfinance.service;

import com.microfinance.dto.response.StaffSummaryResponse;
import com.microfinance.entity.Member;
import com.microfinance.entity.enums.MemberStatus;
import com.microfinance.repository.MemberRepository;
import com.microfinance.repository.PaymentRepository;
import com.microfinance.repository.StaffMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StaffSummaryService {

    private final StaffMemberRepository staffMemberRepository;
    private final MemberRepository memberRepository;
    private final PaymentRepository paymentRepository;

    @Transactional(readOnly = true)
    public List<StaffSummaryResponse> getSummaries() {
        LocalDate today = LocalDate.now();

        Map<Long, BigDecimal> paidByMember = new HashMap<>();
        for (Object[] r : paymentRepository.totalPaidByMember()) {
            paidByMember.put(((Number) r[0]).longValue(), toBigDecimal(r[1]));
        }

        Map<Long, List<Member>> membersByStaff = memberRepository
                .findByStaffMemberIsNotNullAndStatusNot(MemberStatus.CLOSED)
                .stream()
                .collect(Collectors.groupingBy(m -> m.getStaffMember().getId()));

        return staffMemberRepository.findAllByOrderByNameAsc().stream()
                .map(staff -> {
                    List<Member> members = membersByStaff.getOrDefault(staff.getId(), List.of());
                    BigDecimal ot = BigDecimal.ZERO;
                    BigDecimal collection = BigDecimal.ZERO;
                    BigDecimal pending = BigDecimal.ZERO;
                    BigDecimal par = BigDecimal.ZERO;

                    for (Member m : members) {
                        BigDecimal installment = m.getWeeklyAmount() != null ? m.getWeeklyAmount() : BigDecimal.ZERO;
                        BigDecimal gross = totalToCollect(m, installment);
                        BigDecimal paid = paidByMember.getOrDefault(m.getId(), BigDecimal.ZERO);
                        BigDecimal remaining = gross.subtract(paid).max(BigDecimal.ZERO);

                        ot = ot.add(gross);
                        collection = collection.add(paid);
                        pending = pending.add(remaining);

                        BigDecimal expectedByNow = installment.multiply(BigDecimal.valueOf(installmentsDue(m, today)));
                        if (expectedByNow.subtract(paid).signum() > 0) {
                            par = par.add(remaining);
                        }
                    }

                    return StaffSummaryResponse.builder()
                            .staffId(staff.getId())
                            .staffName(staff.getName())
                            .activeClients(members.size())
                            .outstandingAmount(ot)
                            .collectionAmount(collection)
                            .parAmount(par)
                            .pendingAmount(pending)
                            .build();
                })
                .toList();
    }

    /** Total the client is expected to pay over the whole loan: installment x weeks, else the loan amount. */
    private static BigDecimal totalToCollect(Member m, BigDecimal installment) {
        if (m.getTotalWeeks() != null) {
            return installment.multiply(BigDecimal.valueOf(m.getTotalWeeks()));
        }
        return m.getLoanAmount() != null ? m.getLoanAmount() : BigDecimal.ZERO;
    }

    /** How many installments should have been paid by today (first one falls on the join date). */
    private static long installmentsDue(Member m, LocalDate today) {
        LocalDate join = m.getJoinDate();
        if (join == null || today.isBefore(join)) {
            return 0;
        }
        boolean monthly = "MONTHLY".equalsIgnoreCase(m.getPaymentFrequency());
        long elapsed = (monthly ? ChronoUnit.MONTHS.between(join, today)
                                : ChronoUnit.WEEKS.between(join, today)) + 1;
        Integer total = m.getTotalWeeks();
        return total != null ? Math.min(elapsed, total) : elapsed;
    }

    private static BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        return value instanceof BigDecimal b ? b : new BigDecimal(value.toString());
    }
}