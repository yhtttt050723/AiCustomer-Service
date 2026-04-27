package com.ragask.ticketing.model.dto;

import jakarta.validation.constraints.NotBlank;

public record AskTicketRequest(
        @NotBlank(message = "question must not be blank")
        String question,
        String apiKey
) {
}
