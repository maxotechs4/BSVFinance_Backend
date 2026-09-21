package com.microfinance.controller;

import com.microfinance.dto.request.SavingsPaymentRequest;
import com.microfinance.dto.response.ApiResponse;
import com.microfinance.dto.response.SavingsPaymentResponse;
import com.microfinance.service.SavingsPaymentService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/savings-payments")
@RequiredArgsConstructor
@Tag(name = "Savings Payments", description = "Monthly installment payments against a Savings member's plan")
public class SavingsPaymentController {

    private final SavingsPaymentService savingsPaymentService;

    @GetMapping("/member/{savingsMemberId}")
    public ResponseEntity<ApiResponse<List<SavingsPaymentResponse>>> getPaymentsByMember(@PathVariable Long savingsMemberId) {
        return ResponseEntity.ok(ApiResponse.success(savingsPaymentService.getPaymentsBySavingsMember(savingsMemberId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SavingsPaymentResponse>> addPayment(@Valid @RequestBody SavingsPaymentRequest request) {
        SavingsPaymentResponse created = savingsPaymentService.addPayment(request);
        return ResponseEntity.ok(ApiResponse.success("Payment recorded successfully", created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<SavingsPaymentResponse>> updatePayment(
            @PathVariable Long id, @Valid @RequestBody SavingsPaymentRequest request) {
        SavingsPaymentResponse updated = savingsPaymentService.updatePayment(id, request);
        return ResponseEntity.ok(ApiResponse.success("Payment updated successfully", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deletePayment(@PathVariable Long id) {
        savingsPaymentService.deletePayment(id);
        return ResponseEntity.ok(ApiResponse.message("Payment deleted successfully"));
    }
}
