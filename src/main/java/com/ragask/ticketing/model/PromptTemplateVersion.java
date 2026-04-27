package com.ragask.ticketing.model;

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
@Table(name = "prompt_template_versions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromptTemplateVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 128)
    private String promptKey;

    @Column(nullable = false)
    private Integer versionNo;

    @Column(nullable = false, length = 16000)
    private String content;

    @Column(nullable = false)
    private boolean active;

    @Column(nullable = false)
    @Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(length = 255)
    private String changeNote;
}
