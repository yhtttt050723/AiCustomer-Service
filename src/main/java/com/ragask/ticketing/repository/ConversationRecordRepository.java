package com.ragask.ticketing.repository;

import com.ragask.ticketing.model.ConversationRecord;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConversationRecordRepository extends JpaRepository<ConversationRecord, Long> {
    List<ConversationRecord> findTop100ByOrderByCreatedAtDesc();
}
