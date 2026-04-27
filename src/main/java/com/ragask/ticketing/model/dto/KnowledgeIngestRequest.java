package com.ragask.ticketing.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Knowledge ingest request payload.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeIngestRequest {
    @NotBlank(message = "title must not be blank")
    private String title;
    @NotBlank(message = "content must not be blank")
    private String content;
}
