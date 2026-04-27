package com.ragask.ticketing.model.dto;

import com.ragask.ticketing.model.enums.TicketStatus;
import java.time.LocalDateTime;
import java.util.List;

public record EscalationPayload(
        Long ticketId,
        String level,
        double confidence,
        String question,
        String answer,
        List<String> citations,
        TicketStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
