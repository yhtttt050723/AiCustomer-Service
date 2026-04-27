package com.ragask.ticketing.repository;

import com.ragask.ticketing.model.Ticket;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketRepository extends JpaRepository<Ticket, Long> {
    Optional<Ticket> findByIdAndCreatedByUserId(Long id, Long createdByUserId);

    List<Ticket> findByCreatedByUserIdOrderByUpdatedAtDesc(Long createdByUserId);
}
