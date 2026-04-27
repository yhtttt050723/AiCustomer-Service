package com.ragask.ticketing.model.dto;

import jakarta.validation.constraints.NotBlank;

public record StreamAskRequest(
        @NotBlank(message = "question must not be blank")
        String question,
        String sessionId,
        String apiKey
) {
}
