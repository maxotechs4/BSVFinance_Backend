package com.microfinance.controller;

import com.microfinance.dto.request.MemberChargesRequest;
import com.microfinance.dto.request.MemberRequest;
import com.microfinance.dto.response.ApiResponse;
import com.microfinance.dto.response.CollectionSearchResultResponse;
import com.microfinance.dto.response.HeadMemberOptionResponse;
import com.microfinance.dto.response.ImageDataResponse;
import com.microfinance.dto.response.MemberResponse;
import com.microfinance.dto.response.NomineeImageMetaResponse;
import com.microfinance.dto.response.PageResponse;
import com.microfinance.entity.enums.MemberStatus;
import com.microfinance.service.MemberService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
@Tag(name = "Members", description = "Member CRUD, search, and filtering")
public class MemberController {

    private final MemberService memberService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<MemberResponse>>> getMembers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) MemberStatus status,
            @RequestParam(required = false) String filter,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        PageResponse<MemberResponse> result = memberService.getMembers(keyword, status, filter, page, size, sortBy, sortDir);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/heads")
    public ResponseEntity<ApiResponse<List<HeadMemberOptionResponse>>> getHeadMembers() {
        return ResponseEntity.ok(ApiResponse.success(memberService.getHeadMembers()));
    }

    @GetMapping("/heads/{headId}/sub-members")
    public ResponseEntity<ApiResponse<List<MemberResponse>>> getSubMembers(@PathVariable Long headId) {
        return ResponseEntity.ok(ApiResponse.success(memberService.getSubMembers(headId)));
    }

    /** Collection page search box: name/phone/member code matches a specific member; center code matches a whole group. */
    @GetMapping("/collection-search")
    public ResponseEntity<ApiResponse<List<CollectionSearchResultResponse>>> collectionSearch(
            @RequestParam(required = false) String keyword) {
        return ResponseEntity.ok(ApiResponse.success(memberService.searchForCollection(keyword)));
    }

    /**
     * Create Member form: looks up every existing profile with this phone
     * number (any status), so the form can warn about an active duplicate or
     * offer starting a new loan when every match is CLOSED.
     */
    @GetMapping("/check-phone")
    public ResponseEntity<ApiResponse<List<MemberResponse>>> checkPhoneNumber(@RequestParam String phone) {
        return ResponseEntity.ok(ApiResponse.success(memberService.findByPhoneNumber(phone)));
    }

    /** Admin page: list every member (head or sub-member) scheduled for the given weekday. */
    @GetMapping("/by-weekday/{weekday}")
    public ResponseEntity<ApiResponse<List<MemberResponse>>> getMembersByWeekday(@PathVariable String weekday) {
        return ResponseEntity.ok(ApiResponse.success(memberService.getMembersByWeekday(weekday)));
    }

    /** Admin-only (enforced in SecurityConfig): sets a member's insurance and processing charges. */
    @PutMapping("/{id}/charges")
    public ResponseEntity<ApiResponse<MemberResponse>> updateCharges(
            @PathVariable Long id, @Valid @RequestBody MemberChargesRequest request) {
        return ResponseEntity.ok(ApiResponse.success(memberService.updateCharges(id, request)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MemberResponse>> getMemberById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(memberService.getMemberById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<MemberResponse>> createMember(@Valid @RequestBody MemberRequest request) {
        MemberResponse created = memberService.createMember(request);
        return ResponseEntity.status(201).body(ApiResponse.success("Member created", created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<MemberResponse>> updateMember(@PathVariable Long id,
                                                                      @Valid @RequestBody MemberRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Member updated", memberService.updateMember(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteMember(@PathVariable Long id) {
        memberService.deleteMember(id);
        return ResponseEntity.ok(ApiResponse.message("Member deleted"));
    }

    /** Member profile page: closes out a member's loan once it's fully repaid. */
    @PutMapping("/{id}/close-loan")
    public ResponseEntity<ApiResponse<MemberResponse>> closeLoan(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Loan closed", memberService.closeLoan(id)));
    }

    /**
     * Member profile page "Upload Image" button: adds a new nominee photo for
     * this member (multi-image support — never replaces an existing one).
     * Admin-only, enforced in SecurityConfig; Staff/Viewer get a 403.
     */
    @PostMapping(value = "/{id}/nominee-images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<MemberResponse>> uploadNomineeImage(
            @PathVariable Long id, @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(ApiResponse.success("Nominee image uploaded", memberService.uploadNomineeImage(id, file)));
    }

    /** Member profile page "View Image" gallery: metadata for every nominee photo uploaded for this member. Available to every role. */
    @GetMapping("/{id}/nominee-images")
    public ResponseEntity<ApiResponse<List<NomineeImageMetaResponse>>> getNomineeImages(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(memberService.getNomineeImages(id)));
    }

    // ── Member's own photo (passport-size, shown on the printed Loan ────────
    // ── Application, and doubles as ID proof) — a single photo; a new ──────
    // ── upload replaces the previous one, unlike nomineeImages which ───────
    // ── supports many. ──────────────────────────────────────────────────────
        // ── Member's own photo (passport-size, shown on the printed Loan Application; doubles as proof) ──

    /** Member profile page "Upload Photo" button: sets/replaces the member's photo. Admin-only, enforced in SecurityConfig. */
    @PostMapping(value = "/{id}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<MemberResponse>> uploadMemberPhoto(
            @PathVariable Long id, @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(ApiResponse.success("Photo uploaded", memberService.uploadMemberPhoto(id, file)));
    }

    /** Streams the member's photo bytes back with its content type. Available to every role — also used by the print page. */
    @GetMapping("/{id}/photo")
    public ResponseEntity<byte[]> getMemberPhoto(@PathVariable Long id) {
        ImageDataResponse image = memberService.getMemberPhoto(id);
        MediaType mediaType = image.getContentType() != null
                ? MediaType.parseMediaType(image.getContentType())
                : MediaType.APPLICATION_OCTET_STREAM;
        return ResponseEntity.ok().contentType(mediaType).body(image.getData());
    }

    /** Removes the member's photo. Admin-only, enforced in SecurityConfig. */
    @DeleteMapping("/{id}/photo")
    public ResponseEntity<ApiResponse<MemberResponse>> deleteMemberPhoto(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Photo deleted", memberService.deleteMemberPhoto(id)));
    }

    /** Member profile page View/Download actions: streams one specific nominee photo's raw bytes back with its content type. Available to every role. */
    @GetMapping("/{id}/nominee-images/{imageId}")
    public ResponseEntity<byte[]> getNomineeImage(@PathVariable Long id, @PathVariable Long imageId) {
        ImageDataResponse image = memberService.getNomineeImage(id, imageId);
        MediaType mediaType = image.getContentType() != null
                ? MediaType.parseMediaType(image.getContentType())
                : MediaType.APPLICATION_OCTET_STREAM;
        return ResponseEntity.ok().contentType(mediaType).body(image.getData());
    }

    /** Member profile page "Delete" action inside the nominee image preview: permanently removes one photo. Admin-only, enforced in SecurityConfig. */
    @DeleteMapping("/{id}/nominee-images/{imageId}")
    public ResponseEntity<ApiResponse<Void>> deleteNomineeImage(@PathVariable Long id, @PathVariable Long imageId) {
        memberService.deleteNomineeImage(id, imageId);
        return ResponseEntity.ok(ApiResponse.message("Nominee image deleted"));
    }
}