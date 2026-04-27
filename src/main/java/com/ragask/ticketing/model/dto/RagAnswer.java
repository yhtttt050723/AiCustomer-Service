package com.ragask.ticketing.model.dto;

import java.util.List;

public record RagAnswer(
        String answer,
        double confidence,
        List<String> citations,
        List<AttachmentDto> attachments
) {
}
