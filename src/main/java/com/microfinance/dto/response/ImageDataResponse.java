package com.microfinance.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Carries raw image bytes plus their content type between the service layer
 * and MemberController's GET nominee-images/{imageId} endpoint. Never serialized as
 * JSON directly — the controller writes the bytes to the response body with
 * the contentType set as the response's Content-Type header.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImageDataResponse {
    private byte[] data;
    private String contentType;
    private String fileName;
}
