package com.ragask.ticketing.model.dto;

import com.ragask.ticketing.model.ConversationRecord;
import com.ragask.ticketing.model.EscalationEvent;
import java.util.List;

public record SystemPanelResponse(
        String l1Url,
        String l2Url,
        Integer l1Port,
        Integer l2Port,
        List<EscalationEvent> escalationEvents,
        List<ConversationRecord> conversationRecords
) {
}
