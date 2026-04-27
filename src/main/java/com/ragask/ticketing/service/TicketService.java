package com.ragask.ticketing.service;

import com.ragask.ticketing.knowledge.KnowledgeService;
import com.ragask.ticketing.model.Ticket;
import com.ragask.ticketing.model.dto.AttachmentDto;
import com.ragask.ticketing.model.dto.RagAnswer;
import com.ragask.ticketing.model.dto.TicketResponse;
import com.ragask.ticketing.model.enums.TicketStatus;
import com.ragask.ticketing.repository.TicketRepository;
import com.ragask.ticketing.security.UserContext;
import com.ragask.ticketing.common.error.BizException;
import com.ragask.ticketing.common.error.ErrorCode;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;

@Service
@RequiredArgsConstructor
public class TicketService {

    @Value("${handoff.threshold.resolve:0.65}")
    private double aiResolveThreshold;
    @Value("${handoff.threshold.l2:0.35}")
    private double l2ConfidenceThreshold;

    private final TicketRepository ticketRepository;
    private final RagOrchestratorService ragOrchestratorService;
    private final MetricsService metricsService;
    private final KnowledgeService knowledgeService;
    private final EscalationDispatchService escalationDispatchService;
    private final TicketCategoryService ticketCategoryService;

    /**
     * Create a ticket from user's question and run AI/RAG processing.
     *
     * @param question user question
     * @param runtimeApiKey optional runtime API key for model call
     * @return ticket response after AI decision (resolved or escalated)
     */
    public TicketResponse ask(String question, String runtimeApiKey) {
        Long currentUserId = requireCurrentUserId();
        Ticket ticket = new Ticket();
        ticket.setQuestion(question);
        ticket.setStatus(TicketStatus.AI_PROCESSING);
        ticket.setCreatedByUserId(currentUserId);
        ticket = ticketRepository.save(ticket);
        String sessionId = "ticket-" + ticket.getId();

        RagAnswer ragAnswer = ragOrchestratorService.answer(question, sessionId, runtimeApiKey);
        ticket.setAnswer(ragAnswer.getAnswer());
        String category = ticketCategoryService.classify(question, ragAnswer.getCitations(), ragAnswer.getAnswer());
        ticket.setCategory(category);
        ticket.setLastConfidence(ragAnswer.getConfidence());
        ticket.setLastCitations(joinCitations(ragAnswer.getCitations()));

        if (ragAnswer.getConfidence() >= aiResolveThreshold) {
            ticket.setStatus(TicketStatus.RESOLVED);
            ticket.setLatestSummary("AI resolved in bootstrap mode");
            metricsService.countResolvedByAi();
        } else {
            if (ragAnswer.getConfidence() <= l2ConfidenceThreshold) {
                ticket.setStatus(TicketStatus.L2_ASSIGNED);
                boolean sent = escalationDispatchService.dispatchL2(ticket, ragAnswer, ragAnswer.getConfidence());
                ticket.setLatestSummary("Escalated to L2 due to very low confidence, webhookSent=" + sent);
            } else {
                if (category == null || category.isBlank()) {
                    ticket.setStatus(TicketStatus.L1_NEEDS_CATEGORY);
                    ticket.setLatestSummary("Low confidence: L1 handoff requires category (AI classification missing)");
                } else {
                    ticket.setStatus(TicketStatus.L1_ASSIGNED);
                    boolean sent = escalationDispatchService.dispatchL1(ticket, ragAnswer, ragAnswer.getConfidence());
                    ticket.setLatestSummary("Escalated to L1 due to low confidence, webhookSent=" + sent);
                }
            }
        }

        ticket.touch();
        Ticket saved = ticketRepository.save(ticket);
        List<AttachmentDto> attachments = ragAnswer.getConfidence() >= aiResolveThreshold
                ? ragAnswer.getAttachments()
                : List.of();
        return map(saved, ragAnswer.getCitations(), attachments, saved.getCategory(), ragAnswer.getConfidence());
    }

    /**
     * Stream answer tokens for chat-like interaction.
     *
     * @param question user question
     * @param sessionId session identifier
     * @param runtimeApiKey optional runtime API key
     * @return streaming token flux
     */
    public Flux<String> askStream(String question, String sessionId, String runtimeApiKey) {
        String effectiveSession = (sessionId == null || sessionId.isBlank())
                ? "session-default"
                : sessionId;
        return ragOrchestratorService.streamAnswer(question, effectiveSession, runtimeApiKey);
    }

    /**
     * Manually hand off ticket to L1 with mandatory category.
     *
     * @param id ticket id
     * @param reason handoff reason
     * @param category required issue category
     * @return updated ticket response
     */
    public TicketResponse handoffToL1(Long id, String reason, String category) {
        Ticket ticket = get(id);
        if (category == null || category.isBlank()) {
            throw new BizException(ErrorCode.L1_CATEGORY_REQUIRED);
        }
        ticket.setCategory(category);
        ticket.setStatus(TicketStatus.L1_ASSIGNED);
        ticket.setLatestSummary("Handoff to L1: " + reason);
        ticket.touch();
        Ticket saved = ticketRepository.save(ticket);
        RagAnswer stub = new RagAnswer(
                saved.getAnswer() == null ? "" : saved.getAnswer(),
                saved.getLastConfidence(),
                splitCitations(saved.getLastCitations()),
                List.of()
        );
        boolean sent = escalationDispatchService.dispatchL1(saved, stub, saved.getLastConfidence());
        saved.setLatestSummary(saved.getLatestSummary() + ", webhookSent=" + sent);
        Ticket saved2 = ticketRepository.save(saved);
        return map(saved2, splitCitations(saved2.getLastCitations()), List.of(), saved2.getCategory(), saved2.getLastConfidence());
    }

    /**
     * Manually escalate ticket to L2.
     *
     * @param id ticket id
     * @param reason escalation reason
     * @return updated ticket response
     */
    public TicketResponse handoffToL2(Long id, String reason) {
        Ticket ticket = get(id);
        ticket.setStatus(TicketStatus.L2_ASSIGNED);
        ticket.setLatestSummary("Escalated to L2: " + reason);
        ticket.touch();
        return map(ticketRepository.save(ticket), List.of(), List.of(), ticket.getCategory(), 0D);
    }

    /**
     * Mark ticket as resolved and optionally ingest summary to knowledge base.
     *
     * @param id ticket id
     * @param summary resolution summary
     * @param highValue whether to ingest into knowledge base
     * @return updated ticket response
     */
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
        return map(saved, List.of(), List.of(), saved.getCategory(), 1D);
    }

    /**
     * List all tickets.
     *
     * @return ticket list
     */
    public List<TicketResponse> list() {
        Long currentUserId = requireCurrentUserId();
        return ticketRepository.findByCreatedByUserIdOrderByUpdatedAtDesc(currentUserId).stream()
                .map(ticket -> map(ticket, List.of(), List.of(), ticket.getCategory(), 0D))
                .toList();
    }

    private Ticket get(Long id) {
        Long currentUserId = requireCurrentUserId();
        return ticketRepository.findByIdAndCreatedByUserId(id, currentUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "ticket not found"));
    }

    private Long requireCurrentUserId() {
        Long currentUserId = UserContext.getCurrentUserId();
        if (currentUserId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "unauthorized");
        }
        return currentUserId;
    }

    private TicketResponse map(
            Ticket ticket,
            List<String> citations,
            List<AttachmentDto> attachments,
            String category,
            double confidence
    ) {
        return new TicketResponse(
                ticket.getId(),
                ticket.getQuestion(),
                ticket.getAnswer(),
                ticket.getStatus(),
                ticket.isHighValue(),
                citations,
                attachments,
                category,
                confidence,
                ticket.getUpdatedAt()
        );
    }

    private String joinCitations(List<String> citations) {
        if (citations == null || citations.isEmpty()) {
            return "";
        }
        return citations.stream()
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .distinct()
                .collect(Collectors.joining("|"));
    }

    private List<String> splitCitations(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        return java.util.Arrays.stream(raw.split("\\|"))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .distinct()
                .toList();
    }
}
