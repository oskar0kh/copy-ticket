package copy_ticket.copy_ticket.repository;

import copy_ticket.copy_ticket.domain.entity.PublicSeat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PublicSeatRepository extends JpaRepository<PublicSeat, Long> {

	// 특정 라운드에 해당하는 좌석 정보를 좌석 번호 오름차순으로 조회
	@Query(value = """
		SELECT *
		FROM public_seats
		WHERE round_id = :roundId
		ORDER BY seat_number ASC
		""", nativeQuery = true)
	List<PublicSeat> findSeatNumberAscByRoundId(@Param("roundId") Long roundId);
}
