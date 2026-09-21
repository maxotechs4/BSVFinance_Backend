package com.microfinance.service.impl;

import com.microfinance.dto.response.MonthlyReportResponse;
import com.microfinance.dto.response.PaymentResponse;
import com.microfinance.dto.response.WeeklyReportResponse;
import com.microfinance.dto.response.YearlyReportResponse;
import com.microfinance.entity.Payment;
import com.microfinance.entity.enums.PaymentMethod;
import com.microfinance.mapper.PaymentMapper;
import com.microfinance.repository.PaymentRepository;
import com.microfinance.service.ReportService;
import com.microfinance.util.DateUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.IsoFields;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;

    @Override
    @Transactional(readOnly = true)
    public WeeklyReportResponse getWeeklyReport(Integer weekNumber, Integer year, Long memberId, PaymentMethod method) {
        int week = weekNumber != null ? weekNumber : DateUtil.currentIsoWeek();
        int y = year != null ? year : DateUtil.currentIsoWeekYear();

        List<Payment> payments = paymentRepository.findByWeekNumberAndPaymentYear(week, y).stream()
                .filter(p -> memberId == null || p.getMember().getId().equals(memberId))
                .filter(p -> method == null || p.getPaymentMethod() == method)
                .toList();

        BigDecimal total = sum(payments, Payment::getAmountPaid);
        BigDecimal cash = sum(payments.stream().filter(p -> p.getPaymentMethod() == PaymentMethod.CASH).toList(), Payment::getAmountPaid);
        BigDecimal online = sum(payments.stream().filter(p -> p.getPaymentMethod() == PaymentMethod.ONLINE).toList(), Payment::getAmountPaid);
        BigDecimal due = sum(payments, Payment::getRemainingAmount);

        List<PaymentResponse> responses = payments.stream()
                .sorted(Comparator.comparing(Payment::getPaymentDate).reversed())
                .map(paymentMapper::toResponse)
                .toList();

        return WeeklyReportResponse.builder()
                .weekNumber(week)
                .year(y)
                .totalCollection(total)
                .cashCollection(cash)
                .onlineCollection(online)
                .totalDue(due)
                .membersPaid(payments.size())
                .payments(responses)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public MonthlyReportResponse getMonthlyReport(Integer month, Integer year, Long memberId, PaymentMethod method) {
        LocalDate now = LocalDate.now();
        int m = month != null ? month : now.getMonthValue();
        int y = year != null ? year : now.getYear();

        LocalDate start = DateUtil.firstDayOfMonth(y, m);
        LocalDate end = DateUtil.lastDayOfMonth(y, m);

        List<Payment> payments = paymentRepository.findForReport(memberId, method, start, end);

        BigDecimal total = sum(payments, Payment::getAmountPaid);
        BigDecimal cash = sum(payments.stream().filter(p -> p.getPaymentMethod() == PaymentMethod.CASH).toList(), Payment::getAmountPaid);
        BigDecimal online = sum(payments.stream().filter(p -> p.getPaymentMethod() == PaymentMethod.ONLINE).toList(), Payment::getAmountPaid);
        BigDecimal due = sum(payments, Payment::getRemainingAmount);

        Map<Integer, BigDecimal> byWeek = new TreeMap<>();
        for (Payment p : payments) {
            int week = p.getPaymentDate().get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
            byWeek.merge(week, p.getAmountPaid(), BigDecimal::add);
        }

        List<MonthlyReportResponse.WeeklyBreakdown> breakdown = byWeek.entrySet().stream()
                .map(e -> MonthlyReportResponse.WeeklyBreakdown.builder()
                        .weekNumber(e.getKey())
                        .totalCollection(e.getValue())
                        .build())
                .toList();

        List<PaymentResponse> responses = payments.stream()
                .sorted(Comparator.comparing(Payment::getPaymentDate).reversed())
                .map(paymentMapper::toResponse)
                .toList();

        return MonthlyReportResponse.builder()
                .month(m)
                .year(y)
                .totalCollection(total)
                .cashCollection(cash)
                .onlineCollection(online)
                .totalDue(due)
                .weeklyBreakdown(breakdown)
                .payments(responses)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public YearlyReportResponse getYearlyReport(Integer year, Long memberId, PaymentMethod method) {
        int y = year != null ? year : LocalDate.now().getYear();

        LocalDate start = DateUtil.firstDayOfYear(y);
        LocalDate end = DateUtil.lastDayOfYear(y);

        List<Payment> payments = paymentRepository.findForReport(memberId, method, start, end);

        BigDecimal total = sum(payments, Payment::getAmountPaid);
        BigDecimal cash = sum(payments.stream().filter(p -> p.getPaymentMethod() == PaymentMethod.CASH).toList(), Payment::getAmountPaid);
        BigDecimal online = sum(payments.stream().filter(p -> p.getPaymentMethod() == PaymentMethod.ONLINE).toList(), Payment::getAmountPaid);
        BigDecimal due = sum(payments, Payment::getRemainingAmount);

        Map<Integer, BigDecimal> byMonth = new TreeMap<>();
        for (Payment p : payments) {
            int month = p.getPaymentDate().getMonthValue();
            byMonth.merge(month, p.getAmountPaid(), BigDecimal::add);
        }

        List<YearlyReportResponse.MonthlyBreakdown> breakdown = byMonth.entrySet().stream()
                .map(e -> YearlyReportResponse.MonthlyBreakdown.builder()
                        .month(e.getKey())
                        .totalCollection(e.getValue())
                        .build())
                .toList();

        return YearlyReportResponse.builder()
                .year(y)
                .totalCollection(total)
                .cashCollection(cash)
                .onlineCollection(online)
                .totalDue(due)
                .monthlyBreakdown(breakdown)
                .build();
    }

    private BigDecimal sum(List<Payment> payments, java.util.function.Function<Payment, BigDecimal> extractor) {
        return payments.stream().map(extractor).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
