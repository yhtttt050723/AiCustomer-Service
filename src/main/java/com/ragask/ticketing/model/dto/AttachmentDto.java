package com.ragask.ticketing.model.dto;

public record AttachmentDto(
        Long id,
        String fileName,
        String contentType,
        long sizeBytes,
        String downloadUrl
) {
}

