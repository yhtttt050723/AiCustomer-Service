package com.ragask.ticketing.model.dto;

import jakarta.validation.constraints.NotBlank;

public record KnowledgeIngestRequest(
        @NotBlank(message = "title must not be blank")
        String title,
        @NotBlank(message = "content must not be blank")
        String content
) {
}
