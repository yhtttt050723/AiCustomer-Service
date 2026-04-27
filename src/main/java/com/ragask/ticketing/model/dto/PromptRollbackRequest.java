package com.ragask.ticketing.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PromptRollbackRequest(
        @NotBlank String promptKey,
        @NotNull Integer targetVersion,
        String changeNote
) {
}
