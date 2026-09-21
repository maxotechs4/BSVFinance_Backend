package com.microfinance.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StaffMemberResponse {

    private Long id;
    private String name;
    private String phoneNumber;
    private String alternatePhoneNumber;
    private String place;

    private String nominee1Name;
    private String nominee1PhoneNumber;

    private String nominee2Name;
    private String nominee2PhoneNumber;

    private LocalDateTime createdAt;
}
