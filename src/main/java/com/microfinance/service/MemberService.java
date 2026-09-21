package com.microfinance.service;

import com.microfinance.dto.request.MemberChargesRequest;
import com.microfinance.dto.request.MemberRequest;
import com.microfinance.dto.response.CollectionSearchResultResponse;
import com.microfinance.dto.response.HeadMemberOptionResponse;
import com.microfinance.dto.response.ImageDataResponse;
import com.microfinance.dto.response.MemberResponse;
import com.microfinance.dto.response.NomineeImageMetaResponse;
import com.microfinance.dto.response.PageResponse;
import com.microfinance.entity.enums.MemberStatus;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface MemberService {

    /** All existing head members, for the "Head Member" dropdown on the create/edit form. */
    List<HeadMemberOptionResponse> getHeadMembers();

    /** Sub-members belonging to the given head, for the accordion row's expanded state. */
    List<MemberResponse> getSubMembers(Long headId);

    /**
     * Collection page search: matches by member name/phone/member code (returns the
     * specific member that matched) and by center code (returns the whole group's members).
     */
    List<CollectionSearchResultResponse> searchForCollection(String keyword);

    /**
     * Every existing member profile (any status) that has this phone number,
     * used by the Create Member form to warn about an active duplicate, or to
     * offer starting a new loan profile when every match found is CLOSED.
     */
    List<MemberResponse> findByPhoneNumber(String phoneNumber);

    /** Admin-only: sets a member's one-time insurance and processing charges. */
    MemberResponse updateCharges(Long memberId, MemberChargesRequest request);

    MemberResponse createMember(MemberRequest request);

    MemberResponse updateMember(Long id, MemberRequest request);

    void deleteMember(Long id);

    MemberResponse getMemberById(Long id);

    PageResponse<MemberResponse> getMembers(String keyword, MemberStatus status, String filter,
                                             int page, int size, String sortBy, String sortDir);

    List<MemberResponse> getMembersByWeekday(String weekday);

    MemberResponse closeLoan(Long memberId);

    /** Member profile page "Upload Image" (Admin-only): adds a new nominee photo for this member. Multiple images are supported — this never replaces an existing one. */
    MemberResponse uploadNomineeImage(Long id, MultipartFile file);

    /** Member profile page "View Image" gallery: metadata (no bytes) for every nominee photo uploaded for this member. */
    List<NomineeImageMetaResponse> getNomineeImages(Long memberId);

    /** Raw bytes + content type of one specific nominee photo, for View/Download. */
    ImageDataResponse getNomineeImage(Long memberId, Long imageId);

    /** Member profile page "Delete" action inside the nominee image preview: permanently removes one photo. Admin-only, enforced in SecurityConfig. */
    void deleteNomineeImage(Long memberId, Long imageId);

}