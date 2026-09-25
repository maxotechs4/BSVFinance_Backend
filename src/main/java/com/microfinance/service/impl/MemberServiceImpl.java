package com.microfinance.service.impl;
import com.microfinance.dto.response.LoanInstallmentResponse;
import com.microfinance.util.LoanScheduleCalculator;
import com.microfinance.dto.request.MemberChargesRequest;
import com.microfinance.dto.request.MemberRequest;
import com.microfinance.dto.response.CollectionSearchResultResponse;
import com.microfinance.dto.response.HeadMemberOptionResponse;
import com.microfinance.dto.response.ImageDataResponse;
import com.microfinance.dto.response.MemberResponse;
import com.microfinance.dto.response.NomineeImageMetaResponse;
import com.microfinance.dto.response.PageResponse;
import com.microfinance.entity.Member;
import com.microfinance.entity.NomineeImage;
import com.microfinance.entity.Payment;
import com.microfinance.entity.StaffMember;
import com.microfinance.entity.enums.MemberStatus;
import com.microfinance.entity.enums.Weekday;
import com.microfinance.exception.BadRequestException;
import com.microfinance.exception.DuplicateResourceException;
import com.microfinance.exception.ResourceNotFoundException;
import com.microfinance.mapper.MemberMapper;
import com.microfinance.repository.MemberRepository;
import com.microfinance.repository.NomineeImageRepository;
import com.microfinance.repository.PaymentRepository;
import com.microfinance.repository.StaffMemberRepository;
import com.microfinance.service.MemberService;
import com.microfinance.service.CapitalService;
import com.microfinance.util.DateUtil;
import com.microfinance.util.OutstandingCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberServiceImpl implements MemberService {

    private final MemberRepository memberRepository;
    private final PaymentRepository paymentRepository;
    private final StaffMemberRepository staffMemberRepository;
    private final NomineeImageRepository nomineeImageRepository;
    private final MemberMapper memberMapper;
    private final CapitalService capitalService;

    /**
     * Resolves the optional staffMemberId on a create/update request into a
     * managed StaffMember, or null if none was sent. A non-admin request
     * simply never includes this field (enforced client-side in
     * MemberFormDialog), so this method is identical for every role — there's
     * nothing role-specific to check here.
     */
    private StaffMember resolveStaffMember(Long staffMemberId) {
        if (staffMemberId == null) {
            return null;
        }
        return staffMemberRepository.findById(staffMemberId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff member not found with id: " + staffMemberId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<HeadMemberOptionResponse> getHeadMembers() {
        return memberRepository.findByHeadMemberTrueOrderByNameAsc()
                .stream()
                .map(memberMapper::toHeadOption)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MemberResponse> getSubMembers(Long headId) {
        Member head = memberRepository.findById(headId)
                .orElseThrow(() -> new ResourceNotFoundException("Head member not found with id: " + headId));
        if (!head.isHeadMember()) {
            throw new BadRequestException(head.getName() + " is not marked as a head member");
        }
        return memberRepository.findByParentHead_IdOrderByNameAsc(headId)
                .stream()
                .map(m -> enrich(memberMapper.toResponse(m), m))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CollectionSearchResultResponse> searchForCollection(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }
        Pageable limit = PageRequest.of(0, 15);
        List<CollectionSearchResultResponse> results = new ArrayList<>();

        for (Member m : memberRepository.searchIndividualMembers(keyword, limit)) {
            Member head = m.isHeadMember() ? m : m.getParentHead();
            results.add(CollectionSearchResultResponse.builder()
                    .type("MEMBER")
                    .memberId(m.getId())
                    .memberName(m.getName())
                    .memberCode(m.getMemberCode())
                    .phoneNumber(m.getPhoneNumber())
                    .headId(head != null ? head.getId() : null)
                    .headName(head != null ? head.getName() : null)
                    .centerPlace(m.getCenterPlace())
                    .groupId(m.getGroupId())
                    .groupName(m.getGroupName())
                    .build());
        }

            for (Member head : memberRepository.searchGroupsByGroupId(keyword, limit)) {
            List<String> names = new ArrayList<>();
            names.add(head.getName() + " (Head)");
            head.getSubMembers().stream().map(Member::getName).forEach(names::add);
            results.add(CollectionSearchResultResponse.builder()
                    .type("GROUP")
                    .headId(head.getId())
                    .headName(head.getName())
                    .centerPlace(head.getCenterPlace())
                    .groupId(head.getGroupId())
                    .groupName(head.getGroupName())
                    .memberNames(names)
                    .build());
        }

        return results;
    }

    @Override
    @Transactional(readOnly = true)
    public List<MemberResponse> findByPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isBlank()) {
            return List.of();
        }
        return memberRepository.findAllByPhoneNumber(phoneNumber.trim())
                .stream()
                .map(m -> enrich(memberMapper.toResponse(m), m))
                .toList();
    }

    @Override
    @Transactional
    public MemberResponse updateCharges(Long memberId, MemberChargesRequest request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found with id: " + memberId));

        member.setInsuranceAmount(request.getInsuranceAmount());
        member.setProcessingAmount(request.getProcessingAmount());

        Member saved = memberRepository.save(member);
        log.info("Updated charges for member {} - insurance={}, processing={}",
                saved.getMemberCode(), saved.getInsuranceAmount(), saved.getProcessingAmount());
        return enrich(memberMapper.toResponse(saved), saved);
    }

    /** Converts the free-text weekday from the request into the Weekday enum, with a clear error on bad input. */
    private Weekday parseWeekday(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new BadRequestException("Weekday is required");
        }
        try {
            return Weekday.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Weekday must be one of Monday, Tuesday, Wednesday, Thursday, Friday, Saturday, Sunday");
        }
    }

    private Member resolveParentHead(MemberRequest request, Long excludeId) {
        if (request.isHeadMember()) {
            if (request.getHeadMemberId() != null) {
                throw new BadRequestException("A head member cannot be assigned under another head member");
            }
            return null;
        }

        if (request.getHeadMemberId() == null) {
            throw new BadRequestException("Please select the head member this member belongs to");
        }
        if (excludeId != null && request.getHeadMemberId().equals(excludeId)) {
            throw new BadRequestException("A member cannot be set as their own head member");
        }

        Member head = memberRepository.findById(request.getHeadMemberId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Head member not found with id: " + request.getHeadMemberId()));

        if (!head.isHeadMember()) {
            throw new BadRequestException(head.getName() + " is not marked as a head member");
        }
        return head;
    }

    @Override
    @Transactional
    public MemberResponse createMember(MemberRequest request) {
        // A phone number can be reused for a brand new member profile only when
        // every existing profile with that phone number already has a CLOSED
        // loan — i.e. the same person is legitimately starting a fresh loan
        // cycle. If any existing profile with this phone number is still
        // ACTIVE/INACTIVE (loan not closed), block the duplicate.
        if (memberRepository.existsByPhoneNumberAndStatusNot(request.getPhoneNumber(), MemberStatus.CLOSED)) {
            throw new DuplicateResourceException(
                    "A member with phone number " + request.getPhoneNumber()
                            + " already exists and their loan is not closed yet");
        }
        if (request.getMemberCode() == null || request.getMemberCode().isBlank()) {
            throw new BadRequestException("Member ID is required");
        }
        if (memberRepository.existsByMemberCode(request.getMemberCode())) {
            throw new DuplicateResourceException("Member ID " + request.getMemberCode() + " is already in use");
        }

        Member parentHead = resolveParentHead(request, null);

        Member member = Member.builder()
                .memberCode(request.getMemberCode())
                .groupCode(request.isHeadMember() ? request.getGroupCode() : null)
                .name(request.getName())
                .headMember(request.isHeadMember())
                .parentHead(parentHead)
                .centerPlace(request.getCenterPlace())
                .groupId(request.getGroupId())
                .groupName(request.getGroupName())
                .phoneNumber(request.getPhoneNumber())
                .alternatePhoneNumber(request.getAlternatePhoneNumber())
                .marriageStatus(request.getMarriageStatus())
                .gender(request.getGender())
                .dateOfBirth(request.getDateOfBirth())
                .house(request.getHouse())
                .address(request.getAddress())
                .fatherOrHusbandRelation(request.getFatherOrHusbandName() != null && !request.getFatherOrHusbandName().isBlank()
                        ? request.getFatherOrHusbandRelation() : null)
                .fatherOrHusbandName(request.getFatherOrHusbandName())
                .purposeOfLoan(request.getPurposeOfLoan())
                .aadhaarNumber(request.getAadhaarNumber())
                .panNumber(request.getPanNumber())
                .voterId(request.getVoterId())
                .smartCardNumber(request.getSmartCardNumber())
                .bankName(request.getBankName())
                .bankAccountNumber(request.getBankAccountNumber())
                .chequeNumber(request.getChequeNumber())
                .nomineeName(request.getNomineeName())
                .nomineePhoneNumber(request.getNomineePhoneNumber())
                .nomineeRelation(request.getNomineeRelation())
                .nomineeAadhaar(request.getNomineeAadhaar())
                .nomineePan(request.getNomineePan())
                .nomineeVoterId(request.getNomineeVoterId())
                .nomineeGender(request.getNomineeGender())
                .weeklyAmount(request.getWeeklyAmount())
                .loanAmount(request.getLoanAmount())
                .totalWeeks(request.getTotalWeeks())
                .interestPercentage(request.getInterestPercentage() != null ? request.getInterestPercentage() : BigDecimal.ZERO)
                .paymentFrequency(request.getPaymentFrequency() != null ? request.getPaymentFrequency() : "WEEKLY")
                .insuranceAmount(request.getInsuranceAmount() != null ? request.getInsuranceAmount() : BigDecimal.ZERO)
                .processingAmount(request.getProcessingAmount() != null ? request.getProcessingAmount() : BigDecimal.ZERO)
                .joinDate(request.getJoinDate())
                .weekday(parseWeekday(request.getWeekday()))
                .notes(request.getNotes())
                .creditBalance(BigDecimal.ZERO)
                .status(MemberStatus.ACTIVE)
                .staffMember(resolveStaffMember(request.getStaffMemberId()))
                .build();

        // No payments exist yet for a brand new member, so the outstanding
        // amount starts out equal to the total amount to be collected for
        // this loan plan (weeklyAmount * totalWeeks, e.g. 700 x 26 = 18,200).
        member.setOutstandingAmount(
                OutstandingCalculator.calculate(
                        member.getWeeklyAmount(),
                        member.getTotalWeeks(),
                        member.getLoanAmount(),
                        member.getInterestPercentage(),
                        List.of()
                )
        );
        member.setOutstandingAmountWithInterest(
                OutstandingCalculator.calculateWithInterest(
                        member.getWeeklyAmount(),
                        member.getTotalWeeks(),
                        member.getLoanAmount(),
                        member.getInterestPercentage(),
                        List.of()
                )
        );

        Member saved = memberRepository.save(member);
        BigDecimal[] fundingSplit = capitalService.drawForNewLoan(saved.getLoanAmount());
        saved.setCapitalFundedAmount(fundingSplit[0]);
        saved.setCollectionFundedAmount(fundingSplit[1]);
        memberRepository.save(saved);

        log.info("Created member {} ({})", saved.getMemberCode(), saved.getName());
        return enrich(memberMapper.toResponse(saved), saved);
    }

    @Override
    @Transactional
    public MemberResponse updateMember(Long id, MemberRequest request) {
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found with id: " + id));

        if (memberRepository.existsByPhoneNumberAndIdNot(request.getPhoneNumber(), id)) {
            throw new DuplicateResourceException("Another member already uses phone number " + request.getPhoneNumber());
        }

        Member parentHead = resolveParentHead(request, id);

        member.setName(request.getName());
        member.setHeadMember(request.isHeadMember());
        member.setParentHead(parentHead);
        member.setCenterPlace(request.getCenterPlace());
        member.setGroupId(request.getGroupId());
        member.setGroupName(request.getGroupName());
        member.setPhoneNumber(request.getPhoneNumber());
        member.setAlternatePhoneNumber(request.getAlternatePhoneNumber());
        member.setMarriageStatus(request.getMarriageStatus());
        member.setGender(request.getGender());
        member.setDateOfBirth(request.getDateOfBirth());
        member.setHouse(request.getHouse());
        member.setAddress(request.getAddress());
        member.setFatherOrHusbandRelation(request.getFatherOrHusbandName() != null && !request.getFatherOrHusbandName().isBlank()
                ? request.getFatherOrHusbandRelation() : null);
        member.setFatherOrHusbandName(request.getFatherOrHusbandName());
        member.setPurposeOfLoan(request.getPurposeOfLoan());
        member.setAadhaarNumber(request.getAadhaarNumber());
        member.setPanNumber(request.getPanNumber());
        member.setVoterId(request.getVoterId());
        member.setSmartCardNumber(request.getSmartCardNumber());
        member.setBankName(request.getBankName());
        member.setBankAccountNumber(request.getBankAccountNumber());
        member.setChequeNumber(request.getChequeNumber());
        member.setNomineeName(request.getNomineeName());
        member.setNomineePhoneNumber(request.getNomineePhoneNumber());
        member.setNomineeRelation(request.getNomineeRelation());
        member.setNomineeAadhaar(request.getNomineeAadhaar());
        member.setNomineePan(request.getNomineePan());
        member.setNomineeVoterId(request.getNomineeVoterId());
        member.setNomineeGender(request.getNomineeGender());
        member.setWeeklyAmount(request.getWeeklyAmount());
        member.setLoanAmount(request.getLoanAmount());
        member.setTotalWeeks(request.getTotalWeeks());
        if (request.getInterestPercentage() != null) {
            member.setInterestPercentage(request.getInterestPercentage());
        }
        member.setPaymentFrequency(request.getPaymentFrequency() != null ? request.getPaymentFrequency() : "WEEKLY");
        if (request.getInsuranceAmount() != null) {
            member.setInsuranceAmount(request.getInsuranceAmount());
        }
        if (request.getProcessingAmount() != null) {
            member.setProcessingAmount(request.getProcessingAmount());
        }
        member.setJoinDate(request.getJoinDate());
        member.setWeekday(parseWeekday(request.getWeekday()));
        member.setNotes(request.getNotes());
        // Only touched when the request actually carries a value — same
        // convention as insuranceAmount/processingAmount above. STAFF/VIEWER
        // edit payloads never include staffMemberId at all (the field isn't
        // rendered for them), so this guard stops their saves from silently
        // wiping out an existing assignment. To reassign, an admin picks a
        // different staff member in the dropdown, which always sends a value.
        if (request.getStaffMemberId() != null) {
            member.setStaffMember(resolveStaffMember(request.getStaffMemberId()));
        }

        // Loan amount, totalWeeks, and/or interest % may have just changed
        // above, so re-derive the outstanding amount from the member's full
        // payment history rather than trusting whatever was stored before.
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

        Member saved = memberRepository.save(member);
        return enrich(memberMapper.toResponse(saved), saved);
    }

    @Override
    @Transactional
    public void deleteMember(Long id) {
        Member member = memberRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Member not found with id: " + id
                        ));

        if (member.isHeadMember() && !member.getSubMembers().isEmpty()) {
            throw new BadRequestException(
                    "Cannot delete " + member.getName() +
                            " — reassign or remove their " +
                            member.getSubMembers().size() +
                            " sub-member(s) first"
            );
        }

        capitalService.reverseForDeletedLoan(member.getCapitalFundedAmount(), member.getCollectionFundedAmount());

        memberRepository.deleteById(id);
        log.info("Deleted member id={}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public MemberResponse getMemberById(Long id) {
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found with id: " + id));
        return enrich(memberMapper.toResponse(member), member);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<MemberResponse> getMembers(String keyword, MemberStatus status, String filter,
                                                     int page, int size, String sortBy, String sortDir) {

        Sort sort = Sort.by(Sort.Direction.fromOptionalString(sortDir).orElse(Sort.Direction.DESC),
                sortBy == null || sortBy.isBlank() ? "createdAt" : sortBy);

        if ("HEAD".equalsIgnoreCase(filter)) {
            // Dedicated query: matches the head's own fields OR any of their sub-members' names.
            Pageable pageable = PageRequest.of(page, size, sort);
            Page<Member> result = memberRepository.searchHeadsIncludingSubMembers(keyword, status, pageable);
            Page<MemberResponse> mapped = result.map(m -> enrich(memberMapper.toResponse(m), m));
            return PageResponse.of(mapped);
        }

        if (filter != null && !filter.isBlank() && !filter.equalsIgnoreCase("ALL")) {
            List<MemberResponse> all = memberRepository.search(keyword, status, Pageable.unpaged())
                    .getContent()
                    .stream()
                    .map(m -> enrich(memberMapper.toResponse(m), m))
                    .filter(r -> matchesFilter(r, filter))
                    .toList();

            int start = Math.min(page * size, all.size());
            int end = Math.min(start + size, all.size());
            Page<MemberResponse> result = new PageImpl<>(all.subList(start, end), PageRequest.of(page, size, sort), all.size());
            return PageResponse.of(result);
        }

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Member> result = memberRepository.search(keyword, status, pageable);
        Page<MemberResponse> mapped = result.map(m -> enrich(memberMapper.toResponse(m), m));
        return PageResponse.of(mapped);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MemberResponse> getMembersByWeekday(String weekday) {
        Weekday parsed = parseWeekday(weekday);
        return memberRepository.findByWeekdayOrderByNameAsc(parsed)
                .stream()
                .map(m -> enrich(memberMapper.toResponse(m), m))
                .toList();
    }
    private boolean matchesFilter(MemberResponse response, String filter) {
        return switch (filter.toUpperCase()) {
            case "PAID" -> "PAID".equals(response.getCurrentWeekStatus());
            case "PENDING" -> "PENDING".equals(response.getCurrentWeekStatus()) || "PARTIAL".equals(response.getCurrentWeekStatus());
            case "CASH" -> "CASH".equalsIgnoreCase(response.getLastPaymentMethod());
            case "ONLINE" -> "ONLINE".equalsIgnoreCase(response.getLastPaymentMethod());
            case "HEAD" -> response.isHeadMember();
            default -> true;
        };
    }

    /**
     * Enriches a mapped MemberResponse with everything that can't come
     * straight off the entity: current-week payment status, cumulative
     * totals, a fallback outstanding-amount calculation for legacy rows, and
     * the printed repayment schedule (loanSchedule) used by the "Print" /
     * loan-card view on the frontend.
     *
     * Takes the already-loaded Member entity (rather than re-fetching by id)
     * since every call site already has it in hand from the query that
     * produced this response in the first place.
     */
    private MemberResponse enrich(MemberResponse response, Member member) {
        Long memberId = member.getId();
        int week = DateUtil.currentIsoWeek();
        int year = DateUtil.currentIsoWeekYear();

        Optional<Payment> currentWeekPayment = paymentRepository
                .findByMember_IdAndWeekNumberAndPaymentYear(memberId, week, year);

        if (currentWeekPayment.isPresent()) {
            Payment p = currentWeekPayment.get();
            response.setCurrentWeekPaid(p.getAmountPaid());
            response.setRemainingAmount(p.getRemainingAmount());
            response.setCurrentWeekStatus(p.getStatus().name());
            response.setLastPaymentMethod(p.getPaymentMethod().name());
            response.setLastPaymentDate(p.getPaymentDate());
        } else {
            response.setCurrentWeekPaid(BigDecimal.ZERO);
            response.setRemainingAmount(response.getWeeklyAmount());
            response.setCurrentWeekStatus("PENDING");
        }

        // Cumulative totals across every payment ever recorded for this member
        BigDecimal totalPaid = paymentRepository.sumAmountPaidByMemberId(memberId);
        BigDecimal totalBalance = paymentRepository.sumRemainingAmountByMemberId(memberId);

        response.setTotalPaid(totalPaid);
        response.setTotalBalance(totalBalance);
        response.setTotalExpected(totalPaid.add(totalBalance));

        // Members created before this feature existed won't have an
        // outstandingAmount stored yet (column added via ddl-auto=update,
        // which doesn't backfill existing rows). Derive it on the fly here
        // so it always displays correctly; it gets persisted for real the
        // next time this member or one of their payments is saved.
        if (response.getOutstandingAmount() == null || response.getOutstandingAmountWithInterest() == null) {
            List<Payment> allPayments = paymentRepository.findByMember_IdOrderByPaymentYearDescWeekNumberDesc(memberId);
            if (response.getOutstandingAmount() == null) {
                response.setOutstandingAmount(
                        OutstandingCalculator.calculate(
                                response.getWeeklyAmount(),
                                response.getTotalWeeks(),
                                response.getLoanAmount(),
                                response.getInterestPercentage(),
                                allPayments
                        )
                );
            }
            if (response.getOutstandingAmountWithInterest() == null) {
                response.setOutstandingAmountWithInterest(
                        OutstandingCalculator.calculateWithInterest(
                                response.getWeeklyAmount(),
                                response.getTotalWeeks(),
                                response.getLoanAmount(),
                                response.getInterestPercentage(),
                                allPayments
                        )
                );
            }
        }

        // Printed repayment schedule (Loan Card / Repayment Schedule) for the
        // member's "Print" view. Recomputed from the member's current loan
        // terms every time — nothing is persisted, so it always stays in
        // sync if loanAmount/totalWeeks/interestPercentage are edited later.
        // Quietly comes back empty when a member doesn't have enough loan
        // data yet (see LoanScheduleCalculator), which is fine — the print
        // page simply hides that section in that case.
        List<LoanInstallmentResponse> schedule = LoanScheduleCalculator.build(member);
        response.setLoanSchedule(schedule);
        // Member photo (doubles as proof / passport photo): boolean only here
        // (see getMemberPhoto below for the raw bytes, fetched separately).
        response.setHasMemberPhoto(member.getMemberPhotoData() != null && member.getMemberPhotoData().length > 0);
        return response;
    }

    @Override
    @Transactional
    public MemberResponse closeLoan(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found with id: " + memberId));

        if (member.getStatus() == MemberStatus.CLOSED) {
            throw new BadRequestException(member.getName() + "'s loan is already closed");
        }

        // Whether the loan is fully repaid: if a loan amount was recorded at
        // creation, all of it must be paid off; otherwise fall back to there
        // being no outstanding balance across every payment recorded so far.
        BigDecimal totalPaid = paymentRepository.sumAmountPaidByMemberId(memberId);
        BigDecimal totalBalance = paymentRepository.sumRemainingAmountByMemberId(memberId);
        BigDecimal loanAmount = member.getLoanAmount();

        boolean hasRecordedLoanAmount = loanAmount != null && loanAmount.compareTo(BigDecimal.ZERO) > 0;
        boolean fullyRepaid = hasRecordedLoanAmount
                ? totalPaid.compareTo(loanAmount) >= 0
                : totalPaid.compareTo(BigDecimal.ZERO) > 0 && totalBalance.compareTo(BigDecimal.ZERO) <= 0;

        if (!fullyRepaid) {
            BigDecimal outstanding = hasRecordedLoanAmount
                    ? loanAmount.subtract(totalPaid).max(BigDecimal.ZERO)
                    : totalBalance;
            throw new BadRequestException(
                    "Cannot close loan for " + member.getName() + " — outstanding balance of "
                            + outstanding + " remains. Collect all pending payments before closing the loan.");
        }

        member.setStatus(MemberStatus.CLOSED);
        Member saved = memberRepository.save(member);
        log.info("Closed loan for member {} (id={})", saved.getMemberCode(), saved.getId());
        return enrich(memberMapper.toResponse(saved), saved);
    }

    private static final long MAX_NOMINEE_IMAGE_BYTES = 5L * 1024 * 1024; // 5MB
    // ── Member's own photo (passport-size, shown on the printed Loan Application; doubles as proof) ──

    @Override
    @Transactional
    public MemberResponse uploadMemberPhoto(Long id, MultipartFile file) {
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found with id: " + id));

        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Please choose a file to upload");
        }
        if (file.getSize() > MAX_NOMINEE_IMAGE_BYTES) {
            throw new BadRequestException("File is too large — please upload a file under 5MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.toLowerCase().startsWith("image/")) {
            throw new BadRequestException("Only image files (JPG, PNG, etc.) are allowed");
        }

        try {
            member.setMemberPhotoData(file.getBytes());
        } catch (IOException e) {
            throw new BadRequestException("Failed to read the uploaded photo — please try again");
        }
        member.setMemberPhotoContentType(contentType);
        member.setMemberPhotoFileName(file.getOriginalFilename());

        Member saved = memberRepository.save(member);
        log.info("Uploaded photo for {} (id={})", saved.getMemberCode(), saved.getId());
        return enrich(memberMapper.toResponse(saved), saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ImageDataResponse getMemberPhoto(Long id) {
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found with id: " + id));
        if (member.getMemberPhotoData() == null || member.getMemberPhotoData().length == 0) {
            throw new ResourceNotFoundException("No photo uploaded for this member");
        }
        return ImageDataResponse.builder()
                .data(member.getMemberPhotoData())
                .contentType(member.getMemberPhotoContentType())
                .fileName(member.getMemberPhotoFileName())
                .build();
    }

    @Override
    @Transactional
    public MemberResponse deleteMemberPhoto(Long id) {
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found with id: " + id));
        member.setMemberPhotoData(null);
        member.setMemberPhotoContentType(null);
        member.setMemberPhotoFileName(null);
        Member saved = memberRepository.save(member);
        log.info("Deleted photo for {} (id={})", saved.getMemberCode(), saved.getId());
        return enrich(memberMapper.toResponse(saved), saved);
    }
    /**
     * Member profile "Upload Image" (Admin-only, enforced in SecurityConfig).
     * Adds a new nominee photo — members can have any number of them, so this
     * never touches/replaces images uploaded earlier.
     */
    @Override
    @Transactional
    public MemberResponse uploadNomineeImage(Long id, MultipartFile file) {
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found with id: " + id));

        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Please choose an image file to upload");
        }
        if (file.getSize() > MAX_NOMINEE_IMAGE_BYTES) {
            throw new BadRequestException("Image is too large — please upload a file under 5MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.toLowerCase().startsWith("image/")) {
            throw new BadRequestException("Only image files (JPG, PNG, etc.) are allowed");
        }

        byte[] data;
        try {
            data = file.getBytes();
        } catch (IOException e) {
            throw new BadRequestException("Failed to read the uploaded image — please try again");
        }

        NomineeImage image = NomineeImage.builder()
                .member(member)
                .imageData(data)
                .contentType(contentType)
                .fileName(file.getOriginalFilename())
                .build();
        nomineeImageRepository.save(image);

        log.info("Uploaded nominee image for member {} (id={})", member.getMemberCode(), member.getId());
        Member refreshed = memberRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found with id: " + id));
        return enrich(memberMapper.toResponse(refreshed), refreshed);
    }

    /** Member profile "View Image" gallery: metadata only for every photo uploaded for this member. Available to every role. */
    @Override
    @Transactional(readOnly = true)
    public List<NomineeImageMetaResponse> getNomineeImages(Long memberId) {
        if (!memberRepository.existsById(memberId)) {
            throw new ResourceNotFoundException("Member not found with id: " + memberId);
        }
        return nomineeImageRepository.findMetaByMemberId(memberId);
    }

    /** Raw bytes + content type of one specific nominee photo, for the View/Download actions. Scoped to the member so a mismatched member id in the URL 404s instead of leaking another member's photo. */
    @Override
    @Transactional(readOnly = true)
    public ImageDataResponse getNomineeImage(Long memberId, Long imageId) {
        NomineeImage image = nomineeImageRepository.findByIdAndMemberId(imageId, memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Nominee image not found"));

        return ImageDataResponse.builder()
                .data(image.getImageData())
                .contentType(image.getContentType())
                .fileName(image.getFileName())
                .build();
    }

    /**
     * Member profile "Delete" action inside the nominee image preview (Admin-only,
     * enforced in SecurityConfig). Scoped to the member — same as getNomineeImage —
     * so a mismatched member id in the URL 404s instead of deleting another member's photo.
     */
    @Override
    @Transactional
    public void deleteNomineeImage(Long memberId, Long imageId) {
        NomineeImage image = nomineeImageRepository.findByIdAndMemberId(imageId, memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Nominee image not found"));

        nomineeImageRepository.delete(image);
        log.info("Deleted nominee image {} for member id={}", imageId, memberId);
    }
}
