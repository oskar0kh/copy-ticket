package copy_ticket.copy_ticket.repository;

import copy_ticket.copy_ticket.domain.entity.PublicRound;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PublicRoundRepository extends JpaRepository<PublicRound, Long> {

    // round_id에 해당하는 라운드 조회
    Optional<PublicRound> findByRoundId(Integer roundId);

    // 현재 OPEN 상태인 라운드 조회
    @Query(value = """
        SELECT *
        FROM public_rounds
        WHERE status = :status
        ORDER BY updated_at DESC
        LIMIT 1
        """, nativeQuery = true)
    Optional<PublicRound> findOneOpenRound(@Param("status") String status);

    // DB 서버 시각(KST) 기준으로 실제 예매 가능한 OPEN 라운드 1개 조회
    @Query(value = """
        SELECT *
        FROM public_rounds
        WHERE status = 'OPEN'
                    AND open_at <= (CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Seoul')
                    AND close_at > (CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Seoul')
        ORDER BY open_at ASC
        LIMIT 1
        """, nativeQuery = true)
    Optional<PublicRound> findOneBookableOpenRound();
}
