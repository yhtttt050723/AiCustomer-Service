package com.ragask.ticketing.model;

import com.ragask.ticketing.model.enums.TicketStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "tickets")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 2000)
    private String question;

    @Column(length = 8000)
    private String answer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    @Default
    private TicketStatus status = TicketStatus.NEW;

    @Column(nullable = false)
    private boolean highValue;

    @Column(length = 2000)
    private String latestSummary;

    @Column(length = 100)
    private String category;

    @Column(nullable = false)
    private double lastConfidence;

    @Column(length = 6000)
    private String lastCitations;

    @Column(nullable = false)
    @Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    @Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    public void touch() {
        this.updatedAt = LocalDateTime.now();
    }
}
