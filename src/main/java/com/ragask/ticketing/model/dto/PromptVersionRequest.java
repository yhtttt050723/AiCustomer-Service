package com.ragask.ticketing.model.dto;

import jakarta.validation.constraints.NotBlank;

public record PromptVersionRequest(
        @NotBlank String promptKey,
        @NotBlank String content,
        String changeNote,
        boolean activate
) {
}
