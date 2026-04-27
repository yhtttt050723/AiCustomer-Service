package com.ragask.ticketing.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Resolve ticket request payload.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResolveTicketRequest {
    @NotBlank(message = "summary must not be blank")
    private String summary;
    private boolean highValue;
}
