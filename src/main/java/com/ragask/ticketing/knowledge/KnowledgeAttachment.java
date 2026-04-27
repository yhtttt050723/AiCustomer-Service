package com.ragask.ticketing.knowledge;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "knowledge_attachments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Links to knowledge via the same "source" that citations use (e.g. kb://manual/假期申请流程).
     * This keeps attachment lookup stable across chunking/indexing strategies.
     */
    @Column(nullable = false, length = 500)
    private String source;

    @Column(nullable = false, length = 255)
    private String fileName;

    @Column(nullable = false, length = 128)
    private String contentType;

    @Column(nullable = false, length = 1024)
    private String storagePath;

    @Column(nullable = false)
    private long sizeBytes;

    @Column(nullable = false)
    @Default
    private LocalDateTime createdAt = LocalDateTime.now();
}

