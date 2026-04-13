package copy_ticket.copy_ticket.repository;

import copy_ticket.copy_ticket.domain.entity.PublicRound;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface PublicRoundRepository extends JpaRepository<PublicRound, Long> {

    // 현재 OPEN 상태인 라운드 조회
    @Query(value = """
        SELECT *
        FROM public_rounds
        WHERE status = :status
        ORDER BY updated_at DESC
        LIMIT 1
        """, nativeQuery = true)
    Optional<PublicRound> findOneOpenRound(@Param("status") String status);

    // 현재 시각 기준으로 실제 예매 가능한 OPEN 라운드 1개 조회 (KST 로컬 시각 기준: openAt <= now < closeAt)
    @Query(value = """
        SELECT *
        FROM public_rounds
        WHERE status = 'OPEN'
                    AND open_at <= :now
                    AND close_at > :now
        ORDER BY open_at ASC
        LIMIT 1
        """, nativeQuery = true)
    Optional<PublicRound> findOneBookableOpenRound(@Param("now") Instant now);
}
