package com.ragask.ticketing.model.dto;

import com.ragask.ticketing.model.enums.TicketStatus;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Ticket response payload.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TicketResponse {
    private Long id;
    private String question;
    private String answer;
    private TicketStatus status;
    private boolean highValue;
    private List<String> citations;
    private List<AttachmentDto> attachments;
    private String category;
    private double confidence;
    private LocalDateTime updatedAt;
}
