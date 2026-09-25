package com.microfinance.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * Lightweight projection used to populate the "Head Member" dropdown
 * on the create/edit member form. Deliberately excludes the full member
 * payload (documents, bank, nominee, etc.) since the dropdown only needs
 * enough detail for the admin to pick the right head.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HeadMemberOptionResponse {
    private Long id;
    private String memberCode;
    private String name;
    private String centerPlace;
    private String groupId;
    private String groupName;

}
