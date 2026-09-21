package com.microfinance.mapper;

import com.microfinance.dto.response.StaffMemberResponse;
import com.microfinance.entity.StaffMember;
import org.springframework.stereotype.Component;

@Component
public class StaffMemberMapper {

    public StaffMemberResponse toResponse(StaffMember staff) {
        if (staff == null) {
            return null;
        }
        return StaffMemberResponse.builder()
                .id(staff.getId())
                .name(staff.getName())
                .phoneNumber(staff.getPhoneNumber())
                .alternatePhoneNumber(staff.getAlternatePhoneNumber())
                .place(staff.getPlace())
                .nominee1Name(staff.getNominee1Name())
                .nominee1PhoneNumber(staff.getNominee1PhoneNumber())
                .nominee2Name(staff.getNominee2Name())
                .nominee2PhoneNumber(staff.getNominee2PhoneNumber())
                .createdAt(staff.getCreatedAt())
                .build();
    }
}
