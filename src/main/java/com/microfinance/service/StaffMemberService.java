package com.microfinance.service;

import com.microfinance.dto.request.StaffMemberRequest;
import com.microfinance.dto.response.StaffMemberResponse;

import java.util.List;

public interface StaffMemberService {

    List<StaffMemberResponse> getAllStaff();

    StaffMemberResponse getStaffById(Long id);

    StaffMemberResponse createStaff(StaffMemberRequest request);

    StaffMemberResponse updateStaff(Long id, StaffMemberRequest request);

    void deleteStaff(Long id);
}
