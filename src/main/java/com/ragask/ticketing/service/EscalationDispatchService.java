package com.ragask.ticketing.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ragask.ticketing.model.EscalationEvent;
import com.ragask.ticketing.model.Ticket;
import com.ragask.ticketing.model.dto.EscalationPayload;
import com.ragask.ticketing.model.dto.RagAnswer;
import com.ragask.ticketing.repository.EscalationEventRepository;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@Slf4j
public class EscalationDispatchService {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final EscalationEventRepository escalationEventRepository;
    private final String l1Url;
    private final String l2Url;

    public EscalationDispatchService(
            WebClient.Builder webClientBuilder,
            ObjectMapper objectMapper,
            EscalationEventRepository escalationEventRepository,
            @Value("${handoff.l1.url:}") String l1Url,
            @Value("${handoff.l2.url:}") String l2Url
    ) {
        this.webClient = webClientBuilder.build();
        this.objectMapper = objectMapper;
        this.escalationEventRepository = escalationEventRepository;
        this.l1Url = l1Url;
        this.l2Url = l2Url;
    }

    public boolean dispatchL1(Ticket ticket, RagAnswer ragAnswer, double confidence) {
        return dispatch(levelUrl("L1"), "L1", ticket, ragAnswer, confidence);
    }

    public boolean dispatchL2(Ticket ticket, RagAnswer ragAnswer, double confidence) {
        return dispatch(levelUrl("L2"), "L2", ticket, ragAnswer, confidence);
    }

    private String levelUrl(String level) {
        return "L2".equals(level) ? l2Url : l1Url;
    }

    private boolean dispatch(
            String targetUrl,
            String level,
            Ticket ticket,
            RagAnswer ragAnswer,
            double confidence
    ) {
        if (targetUrl == null || targetUrl.isBlank()) {
            return false;
        }
        if ("L1".equals(level) && (ticket.getCategory() == null || ticket.getCategory().isBlank())) {
            // L1 handoff requires category; if missing, do not dispatch.
            saveEvent(ticket.getId(), level, targetUrl, new EscalationPayload(
                    ticket.getId(),
                    level,
                    confidence,
                    null,
                    ticket.getQuestion(),
                    ragAnswer.getAnswer(),
                    ragAnswer.getCitations() == null ? List.of() : ragAnswer.getCitations(),
                    ticket.getStatus(),
                    ticket.getCreatedAt(),
                    ticket.getUpdatedAt()
            ), false);
            return false;
        }
        EscalationPayload payload = new EscalationPayload(
                ticket.getId(),
                level,
                confidence,
                ticket.getCategory(),
                ticket.getQuestion(),
                ragAnswer.getAnswer(),
                ragAnswer.getCitations() == null ? List.of() : ragAnswer.getCitations(),
                ticket.getStatus(),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt()
        );
        try {
            webClient.post()
                    .uri(targetUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(payload)
                    .retrieve()
                    .toBodilessEntity()
                    .block();
            saveEvent(ticket.getId(), level, targetUrl, payload, true);
            return true;
        } catch (Exception ex) {
            log.warn("Escalation dispatch failed: ticketId={}, level={}, targetUrl={}", ticket.getId(), level, targetUrl, ex);
            saveEvent(ticket.getId(), level, targetUrl, payload, false);
            return false;
        }
    }

    public String getL1Url() {
        return l1Url;
    }

    public String getL2Url() {
        return l2Url;
    }

    public List<EscalationEvent> recentEvents() {
        return escalationEventRepository.findTop100ByOrderByCreatedAtDesc();
    }

    private void saveEvent(Long ticketId, String level, String targetUrl, EscalationPayload payload, boolean success) {
        try {
            EscalationEvent event = new EscalationEvent();
            event.setTicketId(ticketId);
            event.setLevel(level);
            event.setTargetUrl(targetUrl);
            event.setSuccess(success);
            event.setPayloadJson(toJson(payload));
            escalationEventRepository.save(event);
        } catch (Exception ex) {
            log.warn("Failed to persist escalation audit event: ticketId={}, level={}", ticketId, level, ex);
        }
    }

    private String toJson(EscalationPayload payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            return "{\"error\":\"payload serialize failed\"}";
        }
    }
}
