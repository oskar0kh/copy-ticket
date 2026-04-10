package copy_ticket.copy_ticket.repository;

import copy_ticket.copy_ticket.domain.entity.PublicRound;
import copy_ticket.copy_ticket.domain.entity.PublicRound.RoundStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PublicRoundRepository extends JpaRepository<PublicRound, Long> {

    // 현재 OPEN 상태인 라운드 조회
    Optional<PublicRound> findByStatus(RoundStatus status);
}
