package com.microfinance.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StaffMemberRequest {

    @NotBlank(message = "Staff name is required")
    @Size(max = 100, message = "Staff name must not exceed 100 characters")
    private String name;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^[6-9]\\d{9}$", message = "Phone number must be a valid 10-digit number")
    private String phoneNumber;

    @Pattern(regexp = "^$|^[6-9]\\d{9}$", message = "Alternate phone number must be a valid 10-digit number")
    private String alternatePhoneNumber;

    @Size(max = 100, message = "Place must not exceed 100 characters")
    private String place;

    // Nominee 1
    @Size(max = 100, message = "Nominee 1 name must not exceed 100 characters")
    private String nominee1Name;

    @Pattern(regexp = "^$|^[6-9]\\d{9}$", message = "Nominee 1 phone number must be a valid 10-digit number")
    private String nominee1PhoneNumber;

    // Nominee 2
    @Size(max = 100, message = "Nominee 2 name must not exceed 100 characters")
    private String nominee2Name;

    @Pattern(regexp = "^$|^[6-9]\\d{9}$", message = "Nominee 2 phone number must be a valid 10-digit number")
    private String nominee2PhoneNumber;
}
