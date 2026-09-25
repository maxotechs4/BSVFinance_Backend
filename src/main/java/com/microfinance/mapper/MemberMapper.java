package com.microfinance.mapper;

import com.microfinance.dto.response.HeadMemberOptionResponse;
import com.microfinance.dto.response.MemberResponse;
import com.microfinance.entity.Member;
import org.springframework.stereotype.Component;

@Component
public class MemberMapper {

    public MemberResponse toResponse(Member member) {
        if (member == null) {
            return null;
        }
        Member parentHead = member.getParentHead();
        return MemberResponse.builder()
                .id(member.getId())
                .memberCode(member.getMemberCode())
                .name(member.getName())
                .headMember(member.isHeadMember())
                .headMemberId(parentHead != null ? parentHead.getId() : null)
                .headMemberName(parentHead != null ? parentHead.getName() : null)
                .subMemberCount(member.getSubMembers() != null ? member.getSubMembers().size() : 0)
                .groupCode(member.getGroupCode())
                .centerPlace(member.getCenterPlace())
                .groupId(member.getGroupId())
                .groupName(member.getGroupName())
                .centerPlace(member.getCenterPlace())
                .phoneNumber(member.getPhoneNumber())
                .alternatePhoneNumber(member.getAlternatePhoneNumber())
                .marriageStatus(member.getMarriageStatus())
                .gender(member.getGender())
                .dateOfBirth(member.getDateOfBirth())
                .house(member.getHouse())
                .address(member.getAddress())
                .fatherOrHusbandRelation(member.getFatherOrHusbandRelation())
                .fatherOrHusbandName(member.getFatherOrHusbandName())
                .purposeOfLoan(member.getPurposeOfLoan())
                .aadhaarNumber(member.getAadhaarNumber())
                .panNumber(member.getPanNumber())
                .voterId(member.getVoterId())
                .smartCardNumber(member.getSmartCardNumber())
                .bankName(member.getBankName())
                .bankAccountNumber(member.getBankAccountNumber())
                .chequeNumber(member.getChequeNumber())
                .nomineeName(member.getNomineeName())
                .nomineePhoneNumber(member.getNomineePhoneNumber())
                .nomineeRelation(member.getNomineeRelation())
                .nomineeAadhaar(member.getNomineeAadhaar())
                .nomineePan(member.getNomineePan())
                .nomineeVoterId(member.getNomineeVoterId())
                .nomineeGender(member.getNomineeGender())
                .nomineeImageCount(member.getNomineeImages() != null ? member.getNomineeImages().size() : 0)
                .hasNomineeImage(member.getNomineeImages() != null && !member.getNomineeImages().isEmpty())
                .weeklyAmount(member.getWeeklyAmount())
                .loanAmount(member.getLoanAmount())
                .totalWeeks(member.getTotalWeeks())
                .interestPercentage(member.getInterestPercentage())
                .outstandingAmount(member.getOutstandingAmount())
                .outstandingAmountWithInterest(member.getOutstandingAmountWithInterest())
                .paymentFrequency(member.getPaymentFrequency())
                .joinDate(member.getJoinDate())
                .weekday(member.getWeekday() != null ? member.getWeekday().name() : null)
                .notes(member.getNotes())
                .creditBalance(member.getCreditBalance())
                .status(member.getStatus())
                .insuranceAmount(member.getInsuranceAmount())
                .processingAmount(member.getProcessingAmount())
                .staffMemberId(member.getStaffMember() != null ? member.getStaffMember().getId() : null)
                .staffMemberName(member.getStaffMember() != null ? member.getStaffMember().getName() : null)
                .createdAt(member.getCreatedAt())
                .build();
    }

    public HeadMemberOptionResponse toHeadOption(Member member) {
        if (member == null) {
            return null;
        }
        return HeadMemberOptionResponse.builder()
                .id(member.getId())
                .memberCode(member.getMemberCode())
                .name(member.getName())
                .centerPlace(member.getCenterPlace())
                .groupId(member.getGroupId())
                .groupName(member.getGroupName())
                .build();
    }
}
