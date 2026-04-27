package com.ragask.ticketing.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Attachment metadata for frontend.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttachmentDto {
    private Long id;
    private String fileName;
    private String contentType;
    private long sizeBytes;
    private String downloadUrl;
}

