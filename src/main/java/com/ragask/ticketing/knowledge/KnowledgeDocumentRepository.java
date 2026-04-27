package com.ragask.ticketing.knowledge;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KnowledgeDocumentRepository extends JpaRepository<KnowledgeDocument, Long> {
    List<KnowledgeDocument> findByTenantId(String tenantId);
}
