package com.microfinance.controller;

import com.microfinance.dto.request.SavingsMemberRequest;
import com.microfinance.dto.response.ApiResponse;
import com.microfinance.dto.response.PageResponse;
import com.microfinance.dto.response.SavingsMemberResponse;
import com.microfinance.entity.enums.MemberStatus;
import com.microfinance.service.SavingsMemberService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/savings-members")
@RequiredArgsConstructor
@Tag(name = "Savings Members", description = "Monthly Savings scheme member CRUD and search")
public class SavingsMemberController {

    private final SavingsMemberService savingsMemberService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<SavingsMemberResponse>>> getSavingsMembers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) MemberStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        return ResponseEntity.ok(ApiResponse.success(
                savingsMemberService.getSavingsMembers(keyword, status, page, size, sortBy, sortDir)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SavingsMemberResponse>> getSavingsMemberById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(savingsMemberService.getSavingsMemberById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SavingsMemberResponse>> createSavingsMember(
            @Valid @RequestBody SavingsMemberRequest request) {
        SavingsMemberResponse created = savingsMemberService.createSavingsMember(request);
        return ResponseEntity.ok(ApiResponse.success("Savings member created successfully", created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<SavingsMemberResponse>> updateSavingsMember(
            @PathVariable Long id, @Valid @RequestBody SavingsMemberRequest request) {
        SavingsMemberResponse updated = savingsMemberService.updateSavingsMember(id, request);
        return ResponseEntity.ok(ApiResponse.success("Savings member updated successfully", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteSavingsMember(@PathVariable Long id) {
        savingsMemberService.deleteSavingsMember(id);
        return ResponseEntity.ok(ApiResponse.message("Savings member deleted successfully"));
    }
}
