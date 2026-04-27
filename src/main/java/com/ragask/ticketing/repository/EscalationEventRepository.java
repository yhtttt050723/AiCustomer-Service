package com.ragask.ticketing.repository;

import com.ragask.ticketing.model.EscalationEvent;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EscalationEventRepository extends JpaRepository<EscalationEvent, Long> {
    List<EscalationEvent> findTop100ByOrderByCreatedAtDesc();
}
