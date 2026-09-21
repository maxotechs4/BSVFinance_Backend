package com.microfinance.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SavingsMemberRequest {

    @NotBlank(message = "Member ID is required")
    @Size(max = 20, message = "Member ID must not exceed 20 characters")
    @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "Member ID can only contain letters, numbers, hyphens, and underscores")
    private String memberCode;

    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name must not exceed 100 characters")
    private String name;

    @Size(max = 100, message = "Place must not exceed 100 characters")
    private String place;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^[6-9]\\d{9}$", message = "Phone number must be a valid 10-digit number")
    private String phoneNumber;

    @Pattern(regexp = "^$|^[6-9]\\d{9}$", message = "Alternate phone number must be a valid 10-digit number")
    private String alternatePhoneNumber;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    private BigDecimal monthlyAmount;

    @NotNull(message = "Join date is required")
    @PastOrPresent(message = "Join date cannot be in the future")
    private LocalDate joinDate;
}
