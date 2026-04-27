package com.ragask.ticketing.model.dto;

import com.ragask.ticketing.model.enums.TicketStatus;
import java.time.LocalDateTime;
import java.util.List;

public record TicketResponse(
        Long id,
        String question,
        String answer,
        TicketStatus status,
        boolean highValue,
        List<String> citations,
        List<AttachmentDto> attachments,
        double confidence,
        LocalDateTime updatedAt
) {
}
