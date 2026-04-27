package com.ragask.ticketing.controller;

import com.ragask.ticketing.model.dto.EscalationPayload;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/handoff")
public class HandoffMockController {

    @PostMapping("/l1")
    public String receiveL1(@Valid @RequestBody EscalationPayload payload) {
        return "ok";
    }

    @PostMapping("/l2")
    public String receiveL2(@Valid @RequestBody EscalationPayload payload) {
        return "ok";
    }
}

