package com.microfinance.service.impl;

import com.microfinance.dto.request.SavingsMemberRequest;
import com.microfinance.dto.response.PageResponse;
import com.microfinance.dto.response.SavingsMemberResponse;
import com.microfinance.entity.SavingsMember;
import com.microfinance.entity.enums.MemberStatus;
import com.microfinance.entity.enums.PaymentStatus;
import com.microfinance.exception.DuplicateResourceException;
import com.microfinance.exception.ResourceNotFoundException;
import com.microfinance.mapper.SavingsMemberMapper;
import com.microfinance.repository.SavingsMemberRepository;
import com.microfinance.repository.SavingsPaymentRepository;
import com.microfinance.service.SavingsMemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class SavingsMemberServiceImpl implements SavingsMemberService {

    private final SavingsMemberRepository savingsMemberRepository;
    private final SavingsPaymentRepository savingsPaymentRepository;
    private final SavingsMemberMapper savingsMemberMapper;

    @Override
    @Transactional
    public SavingsMemberResponse createSavingsMember(SavingsMemberRequest request) {
        if (savingsMemberRepository.existsByMemberCode(request.getMemberCode())) {
            throw new DuplicateResourceException("Member ID " + request.getMemberCode() + " is already in use");
        }
        if (savingsMemberRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new DuplicateResourceException("A savings member with phone number " + request.getPhoneNumber() + " already exists");
        }

        SavingsMember member = SavingsMember.builder()
                .memberCode(request.getMemberCode())
                .name(request.getName())
                .place(request.getPlace())
                .phoneNumber(request.getPhoneNumber())
                .alternatePhoneNumber(request.getAlternatePhoneNumber())
                .monthlyAmount(request.getMonthlyAmount())
                .totalMonths(36)
                .joinDate(request.getJoinDate())
                .status(MemberStatus.ACTIVE)
                .build();

        SavingsMember saved = savingsMemberRepository.save(member);
        log.info("Created savings member {} ({})", saved.getMemberCode(), saved.getName());
        return enrich(savingsMemberMapper.toResponse(saved), saved.getId());
    }

    @Override
    @Transactional
    public SavingsMemberResponse updateSavingsMember(Long id, SavingsMemberRequest request) {
        SavingsMember member = savingsMemberRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Savings member not found with id: " + id));

        if (savingsMemberRepository.existsByPhoneNumberAndIdNot(request.getPhoneNumber(), id)) {
            throw new DuplicateResourceException("Another savings member already uses phone number " + request.getPhoneNumber());
        }

        member.setName(request.getName());
        member.setPlace(request.getPlace());
        member.setPhoneNumber(request.getPhoneNumber());
        member.setAlternatePhoneNumber(request.getAlternatePhoneNumber());
        member.setMonthlyAmount(request.getMonthlyAmount());
        member.setJoinDate(request.getJoinDate());

        SavingsMember saved = savingsMemberRepository.save(member);
        return enrich(savingsMemberMapper.toResponse(saved), saved.getId());
    }

    @Override
    @Transactional
    public void deleteSavingsMember(Long id) {
        if (!savingsMemberRepository.existsById(id)) {
            throw new ResourceNotFoundException("Savings member not found with id: " + id);
        }
        savingsMemberRepository.deleteById(id);
        log.info("Deleted savings member id={}", id);
    }

    @Override
    public SavingsMemberResponse getSavingsMemberById(Long id) {
        SavingsMember member = savingsMemberRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Savings member not found with id: " + id));
        return enrich(savingsMemberMapper.toResponse(member), member.getId());
    }

    @Override
    public PageResponse<SavingsMemberResponse> getSavingsMembers(String keyword, MemberStatus status,
                                                                  int page, int size, String sortBy, String sortDir) {
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        var result = savingsMemberRepository.search(keyword, status, pageable);
        var mapped = result.map(m -> enrich(savingsMemberMapper.toResponse(m), m.getId()));
        return PageResponse.of(mapped);
    }

    private SavingsMemberResponse enrich(SavingsMemberResponse response, Long memberId) {
        BigDecimal totalPaid = savingsPaymentRepository.sumAmountPaidBySavingsMemberId(memberId);
        BigDecimal totalExpected = response.getMonthlyAmount().multiply(BigDecimal.valueOf(response.getTotalMonths()));
        int installmentsPaid = (int) savingsPaymentRepository.countBySavingsMember_IdAndStatus(memberId, PaymentStatus.PAID);

        response.setTotalPaid(totalPaid);
        response.setTotalExpected(totalExpected);
        response.setTotalBalance(totalExpected.subtract(totalPaid).max(BigDecimal.ZERO));
        response.setInstallmentsPaid(installmentsPaid);
        return response;
    }
}
