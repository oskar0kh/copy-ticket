package copy_ticket.copy_ticket.repository;

import copy_ticket.copy_ticket.domain.entity.PublicBooking;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PublicBookingRepository extends JpaRepository<PublicBooking, Long> {
}