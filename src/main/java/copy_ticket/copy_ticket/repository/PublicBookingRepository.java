package copy_ticket.copy_ticket.repository;

import copy_ticket.copy_ticket.domain.entity.PublicBooking;
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
        ORDER BY pb.bookedAt DESC
        """)
    List<PublicBooking> findByUserIdAndRoundId(
            @Param("userId") String userId,
            @Param("roundId") Integer roundId
    );
}