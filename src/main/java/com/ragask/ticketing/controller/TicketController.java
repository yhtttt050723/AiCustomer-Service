package com.ragask.ticketing.controller;

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
public class TicketController {

    private final TicketService ticketService;
    private final MetricsService metricsService;

    public TicketController(TicketService ticketService, MetricsService metricsService) {
        this.ticketService = ticketService;
        this.metricsService = metricsService;
    }

    @PostMapping("/ask")
    public TicketResponse ask(@Valid @RequestBody AskTicketRequest request) {
        return ticketService.ask(request.question(), request.apiKey());
    }

    @PostMapping(value = "/ask/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> askStream(@Valid @RequestBody StreamAskRequest request) {
        return ticketService.askStream(request.question(), request.sessionId(), request.apiKey());
    }

    @PostMapping("/{id}/handoff/l1")
    public TicketResponse handoffL1(@PathVariable Long id, @Valid @RequestBody HandoffRequest request) {
        return ticketService.handoffToL1(id, request.reason());
    }

    @PostMapping("/{id}/handoff/l2")
    public TicketResponse handoffL2(@PathVariable Long id, @Valid @RequestBody HandoffRequest request) {
        return ticketService.handoffToL2(id, request.reason());
    }

    @PostMapping("/{id}/resolve")
    public TicketResponse resolve(@PathVariable Long id, @Valid @RequestBody ResolveTicketRequest request) {
        return ticketService.resolve(id, request.summary(), request.highValue());
    }

    @GetMapping
    public List<TicketResponse> list() {
        return ticketService.list();
    }

    @GetMapping("/metrics/hitrate")
    public HitRateSnapshot hitrate() {
        return metricsService.snapshot();
    }
}
