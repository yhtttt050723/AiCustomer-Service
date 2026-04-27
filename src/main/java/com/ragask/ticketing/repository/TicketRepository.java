package com.ragask.ticketing.repository;

import com.ragask.ticketing.model.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketRepository extends JpaRepository<Ticket, Long> {
}
