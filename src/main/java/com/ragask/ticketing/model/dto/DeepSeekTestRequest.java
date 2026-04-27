package com.ragask.ticketing.model.dto;

import jakarta.validation.constraints.NotBlank;

public record DeepSeekTestRequest(
        @NotBlank String apiKey
) {
}

