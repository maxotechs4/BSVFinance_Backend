package com.microfinance.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Member profile page "View Image" gallery: one row per uploaded nominee
 * photo, metadata only. Raw bytes are fetched separately (and only on
 * demand, for View/Download) via GET
 * /api/members/{id}/nominee-images/{imageId}.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NomineeImageMetaResponse {
    private Long id;
    private String fileName;
    private String contentType;
    private LocalDateTime uploadedAt;
}
