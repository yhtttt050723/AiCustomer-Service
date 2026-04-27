package com.ragask.ticketing.service;

import com.ragask.ticketing.knowledge.KnowledgeService;
import com.ragask.ticketing.model.Ticket;
import com.ragask.ticketing.model.dto.AttachmentDto;
import com.ragask.ticketing.model.dto.RagAnswer;
import com.ragask.ticketing.model.dto.TicketResponse;
import com.ragask.ticketing.model.enums.TicketStatus;
import com.ragask.ticketing.repository.TicketRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;

@Service
public class TicketService {

    private final double aiResolveThreshold;
    private final double l2ConfidenceThreshold;

    private final TicketRepository ticketRepository;
    private final RagOrchestratorService ragOrchestratorService;
    private final MetricsService metricsService;
    private final KnowledgeService knowledgeService;
    private final EscalationDispatchService escalationDispatchService;

    public TicketService(
            TicketRepository ticketRepository,
            RagOrchestratorService ragOrchestratorService,
            MetricsService metricsService,
            KnowledgeService knowledgeService,
            EscalationDispatchService escalationDispatchService,
            @Value("${handoff.threshold.resolve:0.65}") double aiResolveThreshold,
            @Value("${handoff.threshold.l2:0.35}") double l2ConfidenceThreshold
    ) {
        this.ticketRepository = ticketRepository;
        this.ragOrchestratorService = ragOrchestratorService;
        this.metricsService = metricsService;
        this.knowledgeService = knowledgeService;
        this.escalationDispatchService = escalationDispatchService;
        this.aiResolveThreshold = aiResolveThreshold;
        this.l2ConfidenceThreshold = l2ConfidenceThreshold;
    }

    public TicketResponse ask(String question, String runtimeApiKey) {
        Ticket ticket = new Ticket();
        ticket.setQuestion(question);
        ticket.setStatus(TicketStatus.AI_PROCESSING);
        ticket = ticketRepository.save(ticket);
        String sessionId = "ticket-" + ticket.getId();

        RagAnswer ragAnswer = ragOrchestratorService.answer(question, sessionId, runtimeApiKey);
        ticket.setAnswer(ragAnswer.answer());

        if (ragAnswer.confidence() >= aiResolveThreshold) {
            ticket.setStatus(TicketStatus.RESOLVED);
            ticket.setLatestSummary("AI resolved in bootstrap mode");
            metricsService.countResolvedByAi();
        } else {
            if (ragAnswer.confidence() <= l2ConfidenceThreshold) {
                ticket.setStatus(TicketStatus.L2_ASSIGNED);
                boolean sent = escalationDispatchService.dispatchL2(ticket, ragAnswer, ragAnswer.confidence());
                ticket.setLatestSummary("Escalated to L2 due to very low confidence, webhookSent=" + sent);
            } else {
                ticket.setStatus(TicketStatus.L1_ASSIGNED);
                boolean sent = escalationDispatchService.dispatchL1(ticket, ragAnswer, ragAnswer.confidence());
                ticket.setLatestSummary("Escalated to L1 due to low confidence, webhookSent=" + sent);
            }
        }

        ticket.touch();
        Ticket saved = ticketRepository.save(ticket);
        List<AttachmentDto> attachments = ragAnswer.confidence() >= aiResolveThreshold
                ? ragAnswer.attachments()
                : List.of();
        return map(saved, ragAnswer.citations(), attachments, ragAnswer.confidence());
    }

    public Flux<String> askStream(String question, String sessionId, String runtimeApiKey) {
        String effectiveSession = (sessionId == null || sessionId.isBlank())
                ? "session-default"
                : sessionId;
        return ragOrchestratorService.streamAnswer(question, effectiveSession, runtimeApiKey);
    }

    public TicketResponse handoffToL1(Long id, String reason) {
        Ticket ticket = get(id);
        ticket.setStatus(TicketStatus.L1_ASSIGNED);
        ticket.setLatestSummary("Handoff to L1: " + reason);
        ticket.touch();
        return map(ticketRepository.save(ticket), List.of(), List.of(), 0D);
    }

    public TicketResponse handoffToL2(Long id, String reason) {
        Ticket ticket = get(id);
        ticket.setStatus(TicketStatus.L2_ASSIGNED);
        ticket.setLatestSummary("Escalated to L2: " + reason);
        ticket.touch();
        return map(ticketRepository.save(ticket), List.of(), List.of(), 0D);
    }

    public TicketResponse resolve(Long id, String summary, boolean highValue) {
        Ticket ticket = get(id);
        ticket.setStatus(TicketStatus.RESOLVED);
        ticket.setHighValue(highValue);
        ticket.setLatestSummary(summary);
        ticket.touch();
        Ticket saved = ticketRepository.save(ticket);

        if (highValue) {
            knowledgeService.ingestFromTicket(saved.getId(), summary);
        }
        return map(saved, List.of(), List.of(), 1D);
    }

    public List<TicketResponse> list() {
        return ticketRepository.findAll().stream()
                .map(ticket -> map(ticket, List.of(), List.of(), 0D))
                .toList();
    }

    private Ticket get(Long id) {
        return ticketRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "ticket not found"));
    }

    private TicketResponse map(Ticket ticket, List<String> citations, List<AttachmentDto> attachments, double confidence) {
        return new TicketResponse(
                ticket.getId(),
                ticket.getQuestion(),
                ticket.getAnswer(),
                ticket.getStatus(),
                ticket.isHighValue(),
                citations,
                attachments,
                confidence,
                ticket.getUpdatedAt()
        );
    }
}
