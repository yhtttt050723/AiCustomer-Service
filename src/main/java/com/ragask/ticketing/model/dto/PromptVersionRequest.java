package com.ragask.ticketing.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Create prompt version request payload.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PromptVersionRequest {
    @NotBlank
    private String promptKey;
    @NotBlank
    private String content;
    private String changeNote;
    private boolean activate;
}
