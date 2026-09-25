package com.microfinance.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * A single result row for the Collection page's search box.
 *
 * type = "MEMBER": the keyword matched a specific member's name, phone
 *        number, or member code. memberId/memberName identify exactly who
 *        matched; headId/headName identify which group to jump to.
 *
 * type = "GROUP": the keyword matched a center code. headId/headName
 *        identify the group; memberNames lists every member in it (head
 *        first) since the requirement is to show the whole group's members,
 *        not a single matched person.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CollectionSearchResultResponse {
    private String type;
    private Long memberId;
    private String memberName;
    private String memberCode;
    private String phoneNumber;
    private Long headId;
    private String headName;
    private String centerPlace;
    private String groupId;
    private String groupName;
    private List<String> memberNames;
}