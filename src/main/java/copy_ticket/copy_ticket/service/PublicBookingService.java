package copy_ticket.copy_ticket.service;

import copy_ticket.copy_ticket.config.time.KstDateTimeUtils;
import copy_ticket.copy_ticket.domain.entity.PublicBooking;
import copy_ticket.copy_ticket.domain.entity.PublicRound;
import copy_ticket.copy_ticket.domain.entity.PublicSeat;
import copy_ticket.copy_ticket.domain.entity.User;
import copy_ticket.copy_ticket.dto.PublicBookingConfirmRequestDto;
import copy_ticket.copy_ticket.dto.PublicBookingConfirmResponseDto;
import copy_ticket.copy_ticket.dto.PublicBookingMyListResponseDto;
import copy_ticket.copy_ticket.exception.PublicBookingConfirmationException;
import copy_ticket.copy_ticket.repository.PublicBookingRepository;
import copy_ticket.copy_ticket.repository.PublicRoundRepository;
import copy_ticket.copy_ticket.repository.PublicSeatRepository;
import copy_ticket.copy_ticket.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PublicBookingService {

    private static final int MAX_BOOKABLE_SEATS = 4;

    private final PublicRoundRepository publicRoundRepository;
    private final PublicSeatRepository publicSeatRepository;
    private final PublicBookingRepository publicBookingRepository;
    private final UserRepository userRepository;
    private final StringRedisTemplate stringRedisTemplate;

    // 1. 예매 확정 메서드
    @Transactional
    public PublicBookingConfirmResponseDto confirmBooking(PublicBookingConfirmRequestDto request, Authentication authentication) {

        // 1) 인증 정보에서 사용자 ID 추출 및 검증
        if (authentication == null || authentication.getName() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }

        String userId = authentication.getName();
        User user = userRepository.findUserByUserIdWithoutSoftDeleted(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "사용자 정보를 찾을 수 없습니다."));

        // 2) Request에서 seat_id 리스트 추출 및 검증 (예: null 또는 빈 리스트, 중복 ID, 최대 허용 개수 초과 등)
        List<Long> seatIds = normalizeSeatIds(request.getSeatIds());
        if (seatIds.size() > MAX_BOOKABLE_SEATS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "최대 4개의 좌석만 확정할 수 있습니다.");
        }

        // 3) 라운드 유효성 검증
        PublicRound round = publicRoundRepository.findOneBookableOpenRoundById(request.getRoundId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "현재 예매 가능한 라운드가 아닙니다."));

        // 4) LOCKED -> BOOKED 업데이트 & 업데이트된 좌석 수 반환받기
        int updatedCount = publicSeatRepository.confirmBookedSeats(
                round.getId(),
                seatIds,
                user.getUserId(),
                request.getHoldToken()
        );

        // 5) '업데이트된 좌석 수 == 요청된 좌석 수' 일치 검증
        if (updatedCount != seatIds.size()) {

            // 요청한 seatIds 중 현재 라운드에 존재하는 좌석 조회
            List<PublicSeat> seatsInCurrentRound = publicSeatRepository.findSeatIdsByRoundId(round.getId(), seatIds);
            List<Long> failedSeatIds = new ArrayList<>();

            // 조회한 좌석 ID들 중, 현재 라운드에 존재하지 않는 좌석 ID 찾기 -> 실패한 좌석 ID 리스트에 추가 (실패 원인에 '현재 라운드에 존재하지 않는 좌석' 포함)
            for (Long seatId : seatIds) {
                boolean existsInRound = seatsInCurrentRound.stream().anyMatch(seat -> seat.getId().equals(seatId));
                if (!existsInRound) {
                    failedSeatIds.add(seatId);
                }
            }

            // 조회한 좌석 ID들 중, 확정 실패한 좌석 ID 찾기 → 실패한 좌석 ID 리스트에 추가
            List<Long> invalidSeatIds = publicSeatRepository.findInvalidSeatIdsForBooking(
                    round.getId(),
                    seatIds,
                    user.getUserId(),
                    request.getHoldToken()
            );

            // 확정할 수 없는 좌석 ID를 실패한 좌석 ID 리스트에 추가 (중복 방지)
            for (Long seatId : invalidSeatIds) {
                if (!failedSeatIds.contains(seatId)) {
                    failedSeatIds.add(seatId);
                }
            }

            // 실패한 좌석 ID 리스트가 비어있는 경우, 요청된 좌석 ID 전체가 실패한 것으로 간주 -> 요청된 좌석 ID를 실패한 좌석 ID 리스트로 설정
            if (failedSeatIds.isEmpty()) {
                failedSeatIds = seatIds;
            }

            throw new PublicBookingConfirmationException("확정할 수 없는 좌석이 있습니다.", failedSeatIds);
        }

        // 6) 업데이트된 좌석 ID 리스트 조회
        List<PublicSeat> seats = publicSeatRepository.findSeatIdsByRoundId(round.getId(), seatIds);
        Instant now = KstDateTimeUtils.nowInstant();

        // 조회된 좌석 정보를 기반으로 PublicBooking 엔티티 생성 후, public_bookings 테이블에 일괄 저장
        List<PublicBooking> bookings = seats.stream()
                .map(seat -> createBooking(user, round, seat, now))
                .toList();
        publicBookingRepository.saveAll(bookings); // public_bookings 테이블에 저장

        // 7) 트랜잭션 커밋 후 Redis 홀드 키 삭제 등록
        registerRedisCleanup(round.getRoundId(), seatIds);

        // 8) 예매 확정 성공 Response DTO 생성 및 반환
        return PublicBookingConfirmResponseDto.builder()
                .roundId(round.getRoundId())
                .seatIds(seatIds)
                .bookingCount(bookings.size())
            .bookedAt(now)
                .build();
    }

    // 2. 좌석 확정 시, PublicBooking 엔티티 생성 메서드
    private PublicBooking createBooking(User user, PublicRound round, PublicSeat seat, Instant now) {
        return PublicBooking.booked(user, round, seat, now);
    }

    // 3. seatIds 검증 및 정규화 메서드 (예: null 또는 빈 리스트, 중복 ID 제거 등)
    private List<Long> normalizeSeatIds(List<Long> seatIds) {

        // seatIds가 null이거나 빈 리스트인 경우, 예외 발생
        if (seatIds == null || seatIds.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "seatIds는 최소 1개 이상이어야 합니다.");
        }

        // seatIds에서 null 값이 있는지 검증하고, 중복 ID 제거하여 고유한 좌석 ID 리스트 반환
        List<Long> uniqueSeatIds = new ArrayList<>();
        for (Long seatId : seatIds) {
            if (seatId == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "seatId는 null일 수 없습니다.");
            }
            if (!uniqueSeatIds.contains(seatId)) {
                uniqueSeatIds.add(seatId);
            }
        }
        return uniqueSeatIds;
    }

    // 4. 트랜잭션 커밋 후 Redis 홀드 키 삭제 등록 메서드
    private void registerRedisCleanup(Integer roundId, List<Long> seatIds) {

        // 트랜잭션이 활성화되어 있지 않은 경우, 즉시 Redis 홀드 키 삭제
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            deleteRedisHoldKeys(roundId, seatIds);
            return;
        }

        // 트랜잭션 커밋 후에 Redis 홀드 키 삭제 작업 등록
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                deleteRedisHoldKeys(roundId, seatIds);
            }
        });
    }

    // 5. Redis 홀드 키 삭제 메서드
    private void deleteRedisHoldKeys(Integer roundId, List<Long> seatIds) {
        for (Long seatId : seatIds) {
            stringRedisTemplate.delete(buildSeatHoldKey(roundId, seatId));
        }
    }

    // 6. Redis 홀드 키 생성 메서드
    private String buildSeatHoldKey(Integer roundId, Long seatId) {
        return "public-seat:hold:" + roundId + ":" + seatId;
    }

    // 7. 나의 예매내역 조회 메서드
    @Transactional(readOnly = true)
    public PublicBookingMyListResponseDto getMyBookings(Authentication authentication) {

        // 1) 인증 정보 검증
        if (authentication == null || authentication.getName() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }

        String userId = authentication.getName();

        // 2) 사용자 존재 여부 검증
        userRepository.findUserByUserIdWithoutSoftDeleted(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "사용자 정보를 찾을 수 없습니다."));

        // 3) 현재 OPEN 라운드 조회
        PublicRound currentRound = publicRoundRepository.findOneBookableOpenRound()
                .orElse(null);

        // OPEN 라운드가 없으면 빈 결과 반환
        if (currentRound == null) {
            return PublicBookingMyListResponseDto.builder()
                    .roundId(null)
                    .seatIds(new ArrayList<>())
                    .seatNumbers(new ArrayList<>())
                    .bookingCount(0)
                    .bookedAt(null)
                    .build();
        }

        // 4) 사용자의 현재 라운드 예매 내역 조회
        List<PublicBooking> bookings = publicBookingRepository.findByUserIdAndRoundId(userId, currentRound.getRoundId());

        // 예매 내역이 없으면 빈 결과 반환
        if (bookings.isEmpty()) {
            return PublicBookingMyListResponseDto.builder()
                    .roundId(currentRound.getRoundId())
                    .seatIds(new ArrayList<>())
                    .seatNumbers(new ArrayList<>())
                    .bookingCount(0)
                    .bookedAt(null)
                    .build();
        }

        // 5) 예매 내역에서 좌석 ID와 좌석 번호 추출
        List<Long> seatIds = bookings.stream()
                .map(b -> b.getSeat().getId())
                .toList();

        List<String> seatNumbers = bookings.stream()
                .map(PublicBooking::getSeatNumber)
                .toList();

        Instant bookedAt = bookings.get(0).getBookedAt();

        // 6) 응답 DTO 생성 및 반환
        return PublicBookingMyListResponseDto.from(
                currentRound.getRoundId(),
                seatIds,
                seatNumbers,
                bookedAt
        );
    }
}