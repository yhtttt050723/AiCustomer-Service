package com.ragask.ticketing.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Handoff request payload.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HandoffRequest {
    @NotBlank(message = "reason must not be blank")
    private String reason;
    private String category;
}
