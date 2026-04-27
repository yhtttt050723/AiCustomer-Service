package com.ragask.ticketing.model.dto;

import com.ragask.ticketing.model.enums.TicketStatus;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Escalation payload for L1/L2 handoff webhooks.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EscalationPayload {
    private Long ticketId;
    private String level;
    private double confidence;
    private String category;
    private String question;
    private String answer;
    private List<String> citations;
    private TicketStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
