package copy_ticket.copy_ticket.repository;

import copy_ticket.copy_ticket.domain.entity.PublicSeat;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PublicSeatRepository extends JpaRepository<PublicSeat, Long> {
}
