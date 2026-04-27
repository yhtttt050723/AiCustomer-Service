package com.ragask.ticketing.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Ask ticket request payload.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AskTicketRequest {
    @NotBlank(message = "question must not be blank")
    private String question;
    private String apiKey;
}
