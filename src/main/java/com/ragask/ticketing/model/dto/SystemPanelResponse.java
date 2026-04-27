package com.ragask.ticketing.model.dto;

import com.ragask.ticketing.model.ConversationRecord;
import com.ragask.ticketing.model.EscalationEvent;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * System panel response payload.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SystemPanelResponse {
    private String l1Url;
    private String l2Url;
    private Integer l1Port;
    private Integer l2Port;
    private List<EscalationEvent> escalationEvents;
    private List<ConversationRecord> conversationRecords;
}
