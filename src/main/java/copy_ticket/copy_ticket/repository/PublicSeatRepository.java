package copy_ticket.copy_ticket.repository;

import copy_ticket.copy_ticket.domain.entity.PublicSeat;
import org.springframework.data.jpa.repository.Modifying;
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

	// 요청한 seatIds 중 현재 라운드에 존재하는 좌석 조회
	@Query(value = """
		SELECT *
		FROM public_seats
		WHERE round_id = :roundId
		  AND id IN (:seatIds)
		""", nativeQuery = true)
	List<PublicSeat> findSeatIdsByRoundId(@Param("roundId") Long roundId, @Param("seatIds") List<Long> seatIds);

	// 특정 라운드에서 좌석 ID 리스트에 해당하는 좌석 중에서 'AVAILABLE' 상태가 아닌 좌석 ID 조회
	@Query(value = """
		SELECT id
		FROM public_seats
		WHERE round_id = :roundId
		  AND id IN (:seatIds)
		  AND status <> 'AVAILABLE'
		""", nativeQuery = true)
	List<Long> findUnavailableSeatIds(@Param("roundId") Long roundId, @Param("seatIds") List<Long> seatIds);

	// (임시 선점 SQL) 특정 라운드에서 좌석 ID 리스트에 해당하는 좌석을 'AVAILABLE' -> 'LOCKED' 상태로 일괄 업데이트 (실제 홀드 처리)
	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query(value = """
		UPDATE public_seats
		SET status = 'LOCKED',
		    locked_at = (CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Seoul'),
		    locked_by_user_id = :lockedByUserId,
		    hold_token = :holdToken,
		    hold_expires_at = (CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Seoul') + (:holdTtlSeconds * INTERVAL '1 second'),
		    updated_at = (CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Seoul')
		WHERE round_id = :roundId
		  AND id IN (:seatIds)
		  AND status = 'AVAILABLE'
		""", nativeQuery = true)
	int lockAvailableSeats(
			@Param("roundId") Long roundId,
			@Param("seatIds") List<Long> seatIds,
			@Param("lockedByUserId") String lockedByUserId,
			@Param("holdToken") String holdToken,
			@Param("holdTtlSeconds") long holdTtlSeconds
	);

	// 특정 라운드 좌석 리스트 내부의 확정 불가 좌석 Id 조회
	@Query(value = """
		SELECT id
		FROM public_seats
		WHERE round_id = :roundId
		  AND id IN (:seatIds)
		  AND (
			status <> 'LOCKED'
			OR COALESCE(locked_by_user_id, '') <> :lockedByUserId
			OR COALESCE(hold_token, '') <> :holdToken
			OR hold_expires_at IS NULL
			OR hold_expires_at <= (CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Seoul')
		  )
		""", nativeQuery = true)
	List<Long> findInvalidSeatIdsForBooking(
			@Param("roundId") Long roundId,
			@Param("seatIds") List<Long> seatIds,
			@Param("lockedByUserId") String lockedByUserId,
			@Param("holdToken") String holdToken
	);

	// 좌석 확정 시 'LOCKED' -> 'BOOKED' 상태로 일괄 업데이트
	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query(value = """
		UPDATE public_seats
		SET status = 'BOOKED',
		    locked_at = NULL,
		    locked_by_user_id = NULL,
		    hold_token = NULL,
		    hold_expires_at = NULL,
		    updated_at = (CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Seoul')
		WHERE round_id = :roundId
		  AND id IN (:seatIds)
		  AND status = 'LOCKED'
		  AND locked_by_user_id = :lockedByUserId
		  AND hold_token = :holdToken
		  AND hold_expires_at > (CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Seoul')
		""", nativeQuery = true)
	int confirmBookedSeats(
			@Param("roundId") Long roundId,
			@Param("seatIds") List<Long> seatIds,
			@Param("lockedByUserId") String lockedByUserId,
			@Param("holdToken") String holdToken
	);

	// (임시 선점 해제 SQL) 특정 라운드에서 좌석 ID 리스트에 해당하는 좌석을 'LOCKED' -> 'AVAILABLE' 상태로 일괄 업데이트 (실제 홀드 해제 처리)
	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query(value = """
		UPDATE public_seats
		SET status = 'AVAILABLE',
		    locked_at = NULL,
		    locked_by_user_id = NULL,
		    hold_token = NULL,
		    hold_expires_at = NULL,
		    updated_at = (CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Seoul')
		WHERE round_id = :roundId
		  AND id IN (:seatIds)
		  AND status = 'LOCKED'
		  AND locked_by_user_id = :lockedByUserId
		  AND hold_token = :holdToken
		""", nativeQuery = true)
	int releaseLockedSeats(
			@Param("roundId") Long roundId,
			@Param("seatIds") List<Long> seatIds,
			@Param("lockedByUserId") String lockedByUserId,
			@Param("holdToken") String holdToken
	);

	// (TTL 만료 자동 복구 SQL) 모든 라운드에서 hold_expires_at <= now()인 LOCKED 좌석을 AVAILABLE로 자동 복구
	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query(value = """
		UPDATE public_seats
		SET status = 'AVAILABLE',
		    locked_at = NULL,
		    locked_by_user_id = NULL,
		    hold_token = NULL,
		    hold_expires_at = NULL,
		    updated_at = (CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Seoul')
		WHERE status = 'LOCKED'
		  AND hold_expires_at IS NOT NULL
		  AND hold_expires_at <= (CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Seoul')
		""", nativeQuery = true)
	int recoverExpiredLockedSeats();
}
