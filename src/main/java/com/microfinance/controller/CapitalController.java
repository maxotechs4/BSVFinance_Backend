package com.microfinance.controller;

import com.microfinance.dto.request.CapitalTransactionRequest;
import com.microfinance.dto.request.SetCapitalRequest;
import com.microfinance.dto.response.ApiResponse;
import com.microfinance.dto.response.CapitalAccountResponse;
import com.microfinance.dto.response.CapitalTransactionResponse;
import com.microfinance.service.CapitalService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/capital")
@RequiredArgsConstructor
@Tag(name = "Capital", description = "Capital fund balance and manual income/expense entries")
public class CapitalController {

    private final CapitalService capitalService;

    @GetMapping
    public ResponseEntity<ApiResponse<CapitalAccountResponse>> getAccount() {
        return ResponseEntity.ok(ApiResponse.success(capitalService.getAccount()));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<CapitalAccountResponse>> setCapitalAmount(
            @Valid @RequestBody SetCapitalRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Capital amount updated", capitalService.setCapitalAmount(request)));
    }

    @GetMapping("/transactions")
    public ResponseEntity<ApiResponse<List<CapitalTransactionResponse>>> listTransactions() {
        return ResponseEntity.ok(ApiResponse.success(capitalService.listTransactions()));
    }

    @PostMapping("/transactions")
    public ResponseEntity<ApiResponse<CapitalTransactionResponse>> createTransaction(
            @Valid @RequestBody CapitalTransactionRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Entry recorded", capitalService.createTransaction(request)));
    }

    @PutMapping("/transactions/{id}")
    public ResponseEntity<ApiResponse<CapitalTransactionResponse>> updateTransaction(
            @PathVariable Long id,
            @Valid @RequestBody CapitalTransactionRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Entry updated", capitalService.updateTransaction(id, request)));
    }

    @DeleteMapping("/transactions/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTransaction(@PathVariable Long id) {
        capitalService.deleteTransaction(id);
        return ResponseEntity.ok(ApiResponse.message("Entry deleted"));
    }
}
