package copy_ticket.copy_ticket.repository;

import copy_ticket.copy_ticket.domain.entity.PublicRound;
import copy_ticket.copy_ticket.domain.entity.PublicRound.RoundStatus;
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
    Optional<PublicRound> findOpenRound(@Param("status") RoundStatus status);

    // 특정 슬롯 시작 시각의 WAITING 라운드 조회
    @Query(value = """
        SELECT *
        FROM public_rounds
        WHERE status = :status
          AND open_at = :openAt
        ORDER BY updated_at DESC
        LIMIT 1
        """, nativeQuery = true)
    Optional<PublicRound> findWaitingRoundOpenAt(@Param("status") RoundStatus status, @Param("openAt") Instant openAt);

    // openAt이 지났지만 closeAt은 아직 지나지 않은 WAITING 라운드 중 가장 오래된 라운드 조회
    @Query(value = """
        SELECT *
        FROM public_rounds
        WHERE status = 'WAITING'
            AND open_at <= :now
            AND close_at > :now
        ORDER BY open_at ASC
        LIMIT 1
        """, nativeQuery = true)
    Optional<PublicRound> findNotPromotedRound(@Param("now") Instant now);
}
