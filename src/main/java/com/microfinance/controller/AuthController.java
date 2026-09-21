package com.microfinance.controller;

import com.microfinance.dto.request.ChangePasswordRequest;
import com.microfinance.dto.request.ChangeUsernameRequest;
import com.microfinance.dto.request.LoginRequest;
import com.microfinance.dto.response.ApiResponse;
import com.microfinance.dto.response.LoginResponse;
import com.microfinance.service.AuthService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Login and session endpoints")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout() {
        return ResponseEntity.ok(ApiResponse.message("Logged out successfully"));
    }

    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            Authentication authentication) {
        authService.changePassword(authentication.getName(), request);
        return ResponseEntity.ok(ApiResponse.message("Password updated successfully"));
    }

    @PostMapping("/change-username")
    public ResponseEntity<ApiResponse<LoginResponse>> changeUsername(
            @Valid @RequestBody ChangeUsernameRequest request,
            Authentication authentication) {
        LoginResponse response = authService.changeUsername(authentication.getName(), request);
        return ResponseEntity.ok(ApiResponse.success("Username updated successfully", response));
    }
}
