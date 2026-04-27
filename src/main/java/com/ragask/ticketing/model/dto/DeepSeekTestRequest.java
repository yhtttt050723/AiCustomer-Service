package com.ragask.ticketing.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Runtime DeepSeek API key test request payload.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeepSeekTestRequest {
    @NotBlank
    private String apiKey;
}

