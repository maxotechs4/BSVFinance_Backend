package com.microfinance.controller;

import com.microfinance.dto.request.StaffMemberRequest;
import com.microfinance.dto.response.ApiResponse;
import com.microfinance.dto.response.StaffMemberResponse;
import com.microfinance.service.StaffMemberService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Manages field/collection staff records. Every endpoint here is restricted
 * to ADMIN in SecurityConfig ("/api/staff/**" -> hasRole("ADMIN")) — STAFF and
 * VIEWER logins get a 403 even on the read (GET) endpoints, since the Staff
 * tab and the staff-assignment dropdown on Create Member are both admin-only.
 */
@RestController
@RequestMapping("/api/staff")
@RequiredArgsConstructor
@Tag(name = "Staff", description = "Admin-only management of field/collection staff records")
public class StaffMemberController {

    private final StaffMemberService staffMemberService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<StaffMemberResponse>>> getAllStaff() {
        return ResponseEntity.ok(ApiResponse.success(staffMemberService.getAllStaff()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<StaffMemberResponse>> getStaffById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(staffMemberService.getStaffById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<StaffMemberResponse>> createStaff(@Valid @RequestBody StaffMemberRequest request) {
        StaffMemberResponse created = staffMemberService.createStaff(request);
        return ResponseEntity.ok(ApiResponse.success("Staff member created", created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<StaffMemberResponse>> updateStaff(
            @PathVariable Long id, @Valid @RequestBody StaffMemberRequest request) {
        StaffMemberResponse updated = staffMemberService.updateStaff(id, request);
        return ResponseEntity.ok(ApiResponse.success("Staff member updated", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteStaff(@PathVariable Long id) {
        staffMemberService.deleteStaff(id);
        return ResponseEntity.ok(ApiResponse.success("Staff member deleted", null));
    }
}
