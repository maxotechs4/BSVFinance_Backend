package com.microfinance.service.impl;

import com.microfinance.dto.request.SavingsPaymentRequest;
import com.microfinance.dto.response.SavingsPaymentResponse;
import com.microfinance.entity.SavingsMember;
import com.microfinance.entity.SavingsPayment;
import com.microfinance.entity.enums.PaymentMethod;
import com.microfinance.entity.enums.PaymentStatus;
import com.microfinance.exception.BadRequestException;
import com.microfinance.exception.DuplicateResourceException;
import com.microfinance.exception.ResourceNotFoundException;
import com.microfinance.mapper.SavingsPaymentMapper;
import com.microfinance.repository.SavingsMemberRepository;
import com.microfinance.repository.SavingsPaymentRepository;
import com.microfinance.service.SavingsPaymentService;
import com.microfinance.util.SavingsReceiptNumberGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SavingsPaymentServiceImpl implements SavingsPaymentService {

    private static final int MAX_RECEIPT_RETRIES = 5;

    private final SavingsPaymentRepository savingsPaymentRepository;
    private final SavingsMemberRepository savingsMemberRepository;
    private final SavingsPaymentMapper savingsPaymentMapper;
    private final SavingsReceiptNumberGenerator receiptNumberGenerator;

    @Override
    @Transactional
    public SavingsPaymentResponse addPayment(SavingsPaymentRequest request) {
        validateOnlineDetails(request);

        SavingsMember member = savingsMemberRepository.findById(request.getSavingsMemberId())
                .orElseThrow(() -> new ResourceNotFoundException("Savings member not found with id: " + request.getSavingsMemberId()));

        if (request.getInstallmentNumber() > member.getTotalMonths()) {
            throw new BadRequestException("This plan only has " + member.getTotalMonths() + " installments");
        }

        savingsPaymentRepository.findBySavingsMember_IdAndInstallmentNumber(member.getId(), request.getInstallmentNumber())
                .ifPresent(p -> {
                    throw new DuplicateResourceException(
                            "A payment for installment " + request.getInstallmentNumber() +
                                    " already exists for this member. Edit the existing payment instead.");
                });

        BigDecimal monthlyAmount = member.getMonthlyAmount();
        BigDecimal amountPaid = request.getAmountPaid();
        BigDecimal remaining = monthlyAmount.subtract(amountPaid).max(BigDecimal.ZERO);
        BigDecimal extra = amountPaid.subtract(monthlyAmount).max(BigDecimal.ZERO);
        PaymentStatus status = remaining.compareTo(BigDecimal.ZERO) == 0 ? PaymentStatus.PAID : PaymentStatus.PARTIAL;

        SavingsPayment payment = SavingsPayment.builder()
                .savingsMember(member)
                .installmentNumber(request.getInstallmentNumber())
                .monthlyAmount(monthlyAmount)
                .amountPaid(amountPaid)
                .remainingAmount(remaining)
                .extraAmount(extra)
                .status(status)
                .paymentMethod(request.getPaymentMethod())
                .upiTransactionId(request.getPaymentMethod() == PaymentMethod.ONLINE ? request.getUpiTransactionId() : null)
                .paymentDate(request.getPaymentDate())
                .remarks(request.getRemarks())
                .build();

        SavingsPayment saved = saveWithGeneratedReceipt(payment);
        log.info("Recorded savings payment {} for member {} (installment {})",
                saved.getReceiptNumber(), member.getMemberCode(), request.getInstallmentNumber());

        return savingsPaymentMapper.toResponse(saved);
    }

    private SavingsPayment saveWithGeneratedReceipt(SavingsPayment payment) {
        for (int attempt = 0; attempt < MAX_RECEIPT_RETRIES; attempt++) {
            payment.setReceiptNumber(receiptNumberGenerator.generate(attempt));
            try {
                return savingsPaymentRepository.saveAndFlush(payment);
            } catch (DataIntegrityViolationException e) {
                log.warn("Receipt number collision on attempt {}, retrying", attempt);
            }
        }
        throw new IllegalStateException("Could not generate a unique receipt number after " + MAX_RECEIPT_RETRIES + " attempts");
    }

    @Override
    @Transactional
    public SavingsPaymentResponse updatePayment(Long id, SavingsPaymentRequest request) {
        validateOnlineDetails(request);

        SavingsPayment payment = savingsPaymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Savings payment not found with id: " + id));

        SavingsMember member = payment.getSavingsMember();
        BigDecimal monthlyAmount = member.getMonthlyAmount();
        BigDecimal amountPaid = request.getAmountPaid();
        BigDecimal remaining = monthlyAmount.subtract(amountPaid).max(BigDecimal.ZERO);
        BigDecimal extra = amountPaid.subtract(monthlyAmount).max(BigDecimal.ZERO);
        PaymentStatus status = remaining.compareTo(BigDecimal.ZERO) == 0 ? PaymentStatus.PAID : PaymentStatus.PARTIAL;

        payment.setInstallmentNumber(request.getInstallmentNumber());
        payment.setMonthlyAmount(monthlyAmount);
        payment.setAmountPaid(amountPaid);
        payment.setRemainingAmount(remaining);
        payment.setExtraAmount(extra);
        payment.setStatus(status);
        payment.setPaymentMethod(request.getPaymentMethod());
        payment.setUpiTransactionId(request.getPaymentMethod() == PaymentMethod.ONLINE ? request.getUpiTransactionId() : null);
        payment.setPaymentDate(request.getPaymentDate());
        payment.setRemarks(request.getRemarks());

        SavingsPayment saved = savingsPaymentRepository.save(payment);
        return savingsPaymentMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void deletePayment(Long id) {
        SavingsPayment payment = savingsPaymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Savings payment not found with id: " + id));
        savingsPaymentRepository.delete(payment);
        log.info("Deleted savings payment id={}", id);
    }

    @Override
    public List<SavingsPaymentResponse> getPaymentsBySavingsMember(Long savingsMemberId) {
        return savingsPaymentRepository.findBySavingsMember_IdOrderByInstallmentNumberDesc(savingsMemberId)
                .stream()
                .map(savingsPaymentMapper::toResponse)
                .toList();
    }

    private void validateOnlineDetails(SavingsPaymentRequest request) {
        if (request.getPaymentMethod() == PaymentMethod.ONLINE &&
                (request.getUpiTransactionId() == null || request.getUpiTransactionId().isBlank())) {
            throw new BadRequestException("UPI transaction ID is required when payment method is ONLINE");
        }
    }
}
