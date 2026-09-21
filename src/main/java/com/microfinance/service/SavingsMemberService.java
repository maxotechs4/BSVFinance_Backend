package com.microfinance.service;

import com.microfinance.dto.request.SavingsMemberRequest;
import com.microfinance.dto.response.PageResponse;
import com.microfinance.dto.response.SavingsMemberResponse;
import com.microfinance.entity.enums.MemberStatus;

public interface SavingsMemberService {

    SavingsMemberResponse createSavingsMember(SavingsMemberRequest request);

    SavingsMemberResponse updateSavingsMember(Long id, SavingsMemberRequest request);

    void deleteSavingsMember(Long id);

    SavingsMemberResponse getSavingsMemberById(Long id);

    PageResponse<SavingsMemberResponse> getSavingsMembers(String keyword, MemberStatus status,
                                                           int page, int size, String sortBy, String sortDir);
}
