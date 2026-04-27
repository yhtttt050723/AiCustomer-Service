package com.ragask.ticketing.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Rollback prompt version request payload.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PromptRollbackRequest {
    @NotBlank
    private String promptKey;
    @NotNull
    private Integer targetVersion;
    private String changeNote;
}
