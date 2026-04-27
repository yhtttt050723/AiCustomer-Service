package com.ragask.ticketing.controller;

import com.ragask.ticketing.common.api.Result;
import com.ragask.ticketing.model.dto.AskTicketRequest;
import com.ragask.ticketing.model.dto.HandoffRequest;
import com.ragask.ticketing.model.dto.HitRateSnapshot;
import com.ragask.ticketing.model.dto.ResolveTicketRequest;
import com.ragask.ticketing.model.dto.StreamAskRequest;
import com.ragask.ticketing.model.dto.TicketResponse;
import com.ragask.ticketing.service.MetricsService;
import com.ragask.ticketing.service.TicketService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;
    private final MetricsService metricsService;

    @PostMapping("/ask")
    public Result<TicketResponse> ask(@Valid @RequestBody AskTicketRequest request) {
        return Result.ok(ticketService.ask(request.getQuestion(), request.getApiKey()));
    }

    @PostMapping(value = "/ask/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> askStream(@Valid @RequestBody StreamAskRequest request) {
        return ticketService.askStream(request.getQuestion(), request.getSessionId(), request.getApiKey());
    }

    @PostMapping("/{id}/handoff/l1")
    public Result<TicketResponse> handoffL1(@PathVariable Long id, @Valid @RequestBody HandoffRequest request) {
        return Result.ok(ticketService.handoffToL1(id, request.getReason(), request.getCategory()));
    }

    @PostMapping("/{id}/handoff/l2")
    public Result<TicketResponse> handoffL2(@PathVariable Long id, @Valid @RequestBody HandoffRequest request) {
        return Result.ok(ticketService.handoffToL2(id, request.getReason()));
    }

    @PostMapping("/{id}/resolve")
    public Result<TicketResponse> resolve(@PathVariable Long id, @Valid @RequestBody ResolveTicketRequest request) {
        return Result.ok(ticketService.resolve(id, request.getSummary(), request.isHighValue()));
    }

    @GetMapping
    public Result<List<TicketResponse>> list() {
        return Result.ok(ticketService.list());
    }

    @GetMapping("/metrics/hitrate")
    public Result<HitRateSnapshot> hitrate() {
        return Result.ok(metricsService.snapshot());
    }
}
