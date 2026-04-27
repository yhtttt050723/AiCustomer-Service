package com.ragask.ticketing.knowledge;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KnowledgeAttachmentRepository extends JpaRepository<KnowledgeAttachment, Long> {
    List<KnowledgeAttachment> findBySourceOrderByCreatedAtDesc(String source);
}

