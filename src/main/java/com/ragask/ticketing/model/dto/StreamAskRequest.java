package com.ragask.ticketing.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Streaming ask request payload.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StreamAskRequest {
    @NotBlank(message = "question must not be blank")
    private String question;
    private String sessionId;
    private String apiKey;
}
