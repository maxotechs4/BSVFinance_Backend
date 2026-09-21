package com.microfinance.service.impl;

import com.microfinance.dto.request.StaffMemberRequest;
import com.microfinance.dto.response.StaffMemberResponse;
import com.microfinance.entity.StaffMember;
import com.microfinance.exception.ResourceNotFoundException;
import com.microfinance.mapper.StaffMemberMapper;
import com.microfinance.repository.StaffMemberRepository;
import com.microfinance.service.StaffMemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StaffMemberServiceImpl implements StaffMemberService {

    private final StaffMemberRepository staffMemberRepository;
    private final StaffMemberMapper staffMemberMapper;

    @Override
    @Transactional(readOnly = true)
    public List<StaffMemberResponse> getAllStaff() {
        return staffMemberRepository.findAllByOrderByNameAsc()
                .stream()
                .map(staffMemberMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public StaffMemberResponse getStaffById(Long id) {
        StaffMember staff = staffMemberRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Staff member not found with id: " + id));
        return staffMemberMapper.toResponse(staff);
    }

    @Override
    @Transactional
    public StaffMemberResponse createStaff(StaffMemberRequest request) {
        StaffMember staff = StaffMember.builder()
                .name(request.getName())
                .phoneNumber(request.getPhoneNumber())
                .alternatePhoneNumber(request.getAlternatePhoneNumber())
                .place(request.getPlace())
                .nominee1Name(request.getNominee1Name())
                .nominee1PhoneNumber(request.getNominee1PhoneNumber())
                .nominee2Name(request.getNominee2Name())
                .nominee2PhoneNumber(request.getNominee2PhoneNumber())
                .build();

        StaffMember saved = staffMemberRepository.save(staff);
        log.info("Created staff member {} (id={})", saved.getName(), saved.getId());
        return staffMemberMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public StaffMemberResponse updateStaff(Long id, StaffMemberRequest request) {
        StaffMember staff = staffMemberRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Staff member not found with id: " + id));

        staff.setName(request.getName());
        staff.setPhoneNumber(request.getPhoneNumber());
        staff.setAlternatePhoneNumber(request.getAlternatePhoneNumber());
        staff.setPlace(request.getPlace());
        staff.setNominee1Name(request.getNominee1Name());
        staff.setNominee1PhoneNumber(request.getNominee1PhoneNumber());
        staff.setNominee2Name(request.getNominee2Name());
        staff.setNominee2PhoneNumber(request.getNominee2PhoneNumber());

        StaffMember saved = staffMemberRepository.save(staff);
        return staffMemberMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void deleteStaff(Long id) {
        if (!staffMemberRepository.existsById(id)) {
            throw new ResourceNotFoundException("Staff member not found with id: " + id);
        }
        // Any member currently assigned to this staff has their staff_member_id
        // set to NULL automatically at the DB level (see the ON DELETE SET NULL
        // foreign key on Member.staffMember) — deleting a staff record never
        // fails just because customers are still assigned to them.
        staffMemberRepository.deleteById(id);
        log.info("Deleted staff member id={}", id);
    }
}
