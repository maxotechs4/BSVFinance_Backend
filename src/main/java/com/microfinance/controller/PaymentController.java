package com.microfinance.controller;

import com.microfinance.dto.request.CollectionRequest;
import com.microfinance.dto.request.PaymentRequest;
import com.microfinance.dto.response.ApiResponse;
import com.microfinance.dto.response.PageResponse;
import com.microfinance.dto.response.PaymentResponse;
import com.microfinance.entity.enums.PaymentMethod;
import com.microfinance.service.PaymentService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Weekly payment entry, history, and listing")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<ApiResponse<PaymentResponse>> addPayment(@Valid @RequestBody PaymentRequest request) {
        PaymentResponse created = paymentService.addPayment(request);
        return ResponseEntity.status(201).body(ApiResponse.success("Payment recorded", created));
    }

    /**
     * Collection page entry point: saves the amount against the member's next week,
     * auto-derived from their payment history (last week + 1). Updates that week's
     * entry instead of creating a duplicate if it already exists.
     */
    @PostMapping("/collect")
    public ResponseEntity<ApiResponse<PaymentResponse>> collectNextWeekPayment(@Valid @RequestBody CollectionRequest request) {
        PaymentResponse created = paymentService.collectNextWeekPayment(request);
        return ResponseEntity.status(201).body(ApiResponse.success("Collection saved", created));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<PaymentResponse>>> getPayments(
            @RequestParam(required = false) Long memberId,
            @RequestParam(required = false) Integer weekNumber,
            @RequestParam(required = false) Integer paymentYear,
            @RequestParam(required = false) PaymentMethod method,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        PageResponse<PaymentResponse> result = paymentService.getPayments(
                memberId, weekNumber, paymentYear, method, startDate, endDate, page, size);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPaymentById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(paymentService.getPaymentById(id)));
    }

    @GetMapping("/member/{memberId}")
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> getPaymentsByMember(@PathVariable Long memberId) {
        return ResponseEntity.ok(ApiResponse.success(paymentService.getPaymentsByMember(memberId)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PaymentResponse>> updatePayment(@PathVariable Long id,
                                                                        @Valid @RequestBody PaymentRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Payment updated", paymentService.updatePayment(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deletePayment(@PathVariable Long id) {
        paymentService.deletePayment(id);
        return ResponseEntity.ok(ApiResponse.message("Payment deleted"));
    }
}