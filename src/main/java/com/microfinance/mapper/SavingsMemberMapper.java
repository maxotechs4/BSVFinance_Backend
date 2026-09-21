package com.microfinance.mapper;

import com.microfinance.dto.response.SavingsMemberResponse;
import com.microfinance.entity.SavingsMember;
import org.springframework.stereotype.Component;

@Component
public class SavingsMemberMapper {

    public SavingsMemberResponse toResponse(SavingsMember member) {
        if (member == null) {
            return null;
        }
        return SavingsMemberResponse.builder()
                .id(member.getId())
                .memberCode(member.getMemberCode())
                .name(member.getName())
                .place(member.getPlace())
                .phoneNumber(member.getPhoneNumber())
                .alternatePhoneNumber(member.getAlternatePhoneNumber())
                .monthlyAmount(member.getMonthlyAmount())
                .totalMonths(member.getTotalMonths())
                .joinDate(member.getJoinDate())
                .status(member.getStatus())
                .createdAt(member.getCreatedAt())
                .build();
    }
}
