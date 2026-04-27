package com.ragask.ticketing.model.dto;

import jakarta.validation.constraints.NotBlank;

public record HandoffRequest(
        @NotBlank(message = "reason must not be blank")
        String reason
) {
}
