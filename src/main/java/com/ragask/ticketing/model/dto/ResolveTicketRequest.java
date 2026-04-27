package com.ragask.ticketing.model.dto;

import jakarta.validation.constraints.NotBlank;

public record ResolveTicketRequest(
        @NotBlank(message = "summary must not be blank")
        String summary,
        boolean highValue
) {
}
