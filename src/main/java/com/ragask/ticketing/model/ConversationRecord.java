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
@Table(name = "conversation_records")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 128)
    private String sessionId;

    @Column(nullable = false, length = 32)
    private String role;

    @Column(nullable = false, length = 8000)
    private String content;

    @Column(nullable = false)
    @Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
