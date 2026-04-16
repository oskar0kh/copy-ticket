package copy_ticket.copy_ticket.repository;

import copy_ticket.copy_ticket.domain.entity.PublicBooking;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PublicBookingRepository extends JpaRepository<PublicBooking, Long> {

    // 사용자 ID와 라운드 ID에 해당하는 예매 내역 조회 (최근 예매 내역이 가장 먼저 오도록 정렬)
    @Query("""
        SELECT pb FROM PublicBooking pb
        WHERE pb.user.userId = :userId
        AND pb.round.roundId = :roundId
        AND pb.deletedAt IS NULL
        ORDER BY pb.bookedAt DESC
        """)
    List<PublicBooking> findByUserIdAndRoundId(
            @Param("userId") String userId,
            @Param("roundId") Integer roundId
    );

    // 라운드 ID에 해당하는 booking 내역 soft delete (deleted_at 컬럼에 삭제 시각 기록)
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
        UPDATE public_bookings
        SET deleted_at = (CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Seoul')
        WHERE round_id = :roundId
          AND deleted_at IS NULL
        """, nativeQuery = true)
    int softDeleteByRoundId(@Param("roundId") Long roundId);
}