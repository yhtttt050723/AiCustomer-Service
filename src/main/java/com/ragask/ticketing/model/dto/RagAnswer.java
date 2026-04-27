package com.ragask.ticketing.model.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * RAG answer output from orchestrator.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RagAnswer {
    private String answer;
    private double confidence;
    private List<String> citations;
    private List<AttachmentDto> attachments;
}
