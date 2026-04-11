package copy_ticket.copy_ticket.repository;

import copy_ticket.copy_ticket.domain.entity.PublicRound;
import copy_ticket.copy_ticket.domain.entity.PublicRound.RoundStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface PublicRoundRepository extends JpaRepository<PublicRound, Long> {

    boolean existsByStatus(RoundStatus status);

    // 현재 OPEN 상태인 라운드 조회
    @Query(value = """
        SELECT *
        FROM public_rounds
        WHERE status = :status
        ORDER BY updated_at DESC
        LIMIT 1
        """, nativeQuery = true)
    Optional<PublicRound> findOpenRound(@Param("status") String status);

    // 특정 슬롯 시작 시각의 WAITING 라운드 조회
    @Query(value = """
        SELECT *
        FROM public_rounds
        WHERE status = :status
          AND open_at = :openAt
        ORDER BY updated_at DESC
        LIMIT 1
        """, nativeQuery = true)
    Optional<PublicRound> findWaitingRoundOpenAt(@Param("status") String status, @Param("openAt") Instant openAt);

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

    // 현재 시각 기준으로 실제 예매 가능한 OPEN 라운드 1개 조회 (openAt <= now < closeAt)
    @Query(value = """
        SELECT *
        FROM public_rounds
        WHERE status = 'OPEN'
          AND open_at <= :now
          AND close_at > :now
        ORDER BY open_at ASC
        LIMIT 1
        """, nativeQuery = true)
    Optional<PublicRound> findBookableOpenRound(@Param("now") Instant now);
}
