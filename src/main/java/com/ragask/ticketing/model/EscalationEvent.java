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
@Table(name = "escalation_events")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EscalationEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long ticketId;

    @Column(nullable = false, length = 8)
    private String level;

    @Column(nullable = false, length = 1000)
    private String targetUrl;

    @Column(nullable = false)
    private boolean success;

    @Column(nullable = false, length = 16000)
    private String payloadJson;

    @Column(nullable = false)
    @Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
