package copy_ticket.copy_ticket.service;

import copy_ticket.copy_ticket.config.time.KstDateTimeUtils;
import copy_ticket.copy_ticket.domain.entity.PublicRound;
import copy_ticket.copy_ticket.domain.entity.PublicSeat;
import copy_ticket.copy_ticket.domain.entity.User;
import copy_ticket.copy_ticket.dto.PublicSeatHoldRequestDto;
import copy_ticket.copy_ticket.dto.PublicSeatHoldReleaseRequestDto;
import copy_ticket.copy_ticket.dto.PublicSeatHoldResponseDto;
import copy_ticket.copy_ticket.dto.PublicSeatResponseDto;
import copy_ticket.copy_ticket.repository.PublicRoundRepository;
import copy_ticket.copy_ticket.repository.PublicSeatRepository;
import copy_ticket.copy_ticket.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PublicSeatService {

    private static final int MAX_HOLDABLE_SEATS = 4;
    private static final Duration HOLD_TTL = Duration.ofMinutes(5);

    private final PublicRoundRepository publicRoundRepository;
    private final PublicSeatRepository publicSeatRepository;
    private final UserRepository userRepository;
    private final StringRedisTemplate stringRedisTemplate;

    // 1. 라운드 ID에 해당하는 좌석 정보 조회
    @Transactional(readOnly = true)
    public List<PublicSeatResponseDto> getSeatsByRoundId(Integer roundId) {

        // 라운드 ID에 해당하는 라운드 조회 (존재하지 않으면 404 NOT FOUND)
        PublicRound round = publicRoundRepository.findByRoundId(roundId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "현재 라운드를 찾을 수 없습니다."));

        // 라운드에 해당하는 좌석 정보 조회 후, 'PublicSeat' 엔티티 list로 반환 (좌석 번호 오름차순 정렬)
        List<PublicSeat> seats = publicSeatRepository.findSeatNumberAscByRoundId(round.getId());

        // 'PublicSeat' 엔티티 list -> Response DTO로 변환
        return seats.stream()
                .map(this::toDto)
                .toList();
    }

    // 2. 'PublicSeat' 엔티티 -> PublicSeatResponseDto 변환
    private PublicSeatResponseDto toDto(PublicSeat seat) {

        // 좌석 번호에서 displayOrder 추출 (예: "S1" -> 1, "S20" -> 20, "A1" -> 0)
        int displayOrder = parseDisplayOrder(seat.getSeatNumber());
        return PublicSeatResponseDto.of(seat, displayOrder);
    }

    // 3. 좌석 번호에서 displayOrder 추출 (예: "S1" -> 1, "S20" -> 20, "A1" -> 0)
    private int parseDisplayOrder(String seatNumber) {
        
        // 좌석 번호가 "S"로 시작하지 않거나 숫자 부분이 없는 경우, displayOrder를 0으로 처리
        if (seatNumber == null || !seatNumber.startsWith("S")) {
            return 0;
        }

        // "S" 다음의 숫자 부분을 추출하여 displayOrder로 사용 (예: "S1" -> 1, "S20" -> 20)
        try {
            return Integer.parseInt(seatNumber.substring(1));
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    // 4. 프론트에서 건낸 좌석들의 status를 ‘AVAILABLE → LOCKED’로 UPDATE하고, 홀드 토큰 발급 및 '선택 좌석 확인' 화면 진입
    @Transactional
    public PublicSeatHoldResponseDto holdSeats(PublicSeatHoldRequestDto request, Authentication authentication) {

        // authentication 발급 여부 확인
        if (authentication == null || authentication.getName() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }

        // 인증 객체에서 user_id 추출 및 사용자 조회 (인증 정보가 없거나 사용자 정보가 존재하지 않으면 401 UNAUTHORIZED)
        String userId = authentication.getName();
        User user = userRepository.findUserByUserIdWithoutSoftDeleted(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "사용자 정보를 찾을 수 없습니다."));

        // 프론트 Request에서 seat_id List 추출 및 중복 제거/유효성 검사 (최대 4개 좌석, null 또는 빈 리스트 허용하지 않음)
        List<Long> seatIds = normalizeSeatIds(request.getSeatIds());
        if (seatIds.size() > MAX_HOLDABLE_SEATS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "최대 4개의 좌석만 선택할 수 있습니다.");
        }

        // OPEN 라운드 중에서 round_id에 해당하는 라운드 조회 (존재하지 않으면 409 CONFLICT)
        PublicRound round = publicRoundRepository.findOneBookableOpenRoundById(request.getRoundId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "현재 예매 가능한 라운드가 아닙니다."));

        // 해당 라운드에서 seat_id List에 해당하는 좌석 정보 조회 (존재하지 않는 좌석 ID가 포함된 경우 404 NOT FOUND)
        List<PublicSeat> seats = publicSeatRepository.findSeatIdsByRoundId(round.getId(), seatIds);
        if (seats.size() != seatIds.size()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "요청한 좌석 중 존재하지 않는 좌석이 있습니다.");
        }

        Instant now = KstDateTimeUtils.nowInstant();
        Instant holdExpiresAt = now.plus(HOLD_TTL);      // 홀드 만료 시각 계산 (현재 시각 + 5분)
        String holdToken = UUID.randomUUID().toString(); // 홀드 토큰 생성 (예: UUID)

        // 해당 라운드에서 seat_id List에 해당하는 좌석을 'AVAILABLE' -> 'LOCKED' 상태로 일괄 업데이트 (실제 홀드 처리)
        int lockedCount = publicSeatRepository.lockAvailableSeats(
                round.getId(),
                seatIds,
                user.getUserId(),
                holdToken,
            HOLD_TTL.getSeconds()
        );

        // '업데이트된 좌석 수 != 요청한 seat_id 개수' 일때 : 이미 선점되었거나 예매 완료된 좌석이 포함된 것으로 간주 -> 409 CONFLICT 응답
        if (lockedCount != seatIds.size()) {
            List<Long> unavailableSeatIds = publicSeatRepository.findUnavailableSeatIds(round.getId(), seatIds);
            String message = unavailableSeatIds.isEmpty()
                    ? "이미 선점되었거나 예매 완료된 좌석이 포함되어 있습니다."
                    : "선택한 좌석 중 선점 불가 좌석이 있습니다: " + unavailableSeatIds;
            throw new ResponseStatusException(HttpStatus.CONFLICT, message);
        }

        // Redis에 홀드 정보 저장 (키: "public-seat:hold:{roundId}:{seatId}", 값: holdToken, TTL: 5분)
        //  * 실제 선점 상태 관리는 DB의 좌석 상태 + Redis 캐시로 이중화
        try {
            for (Long seatId : seatIds) {
                String redisKey = buildSeatHoldKey(round.getRoundId(), seatId);
                stringRedisTemplate.opsForValue().set(redisKey, holdToken, HOLD_TTL);
            }
        } catch (DataAccessException exception) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "좌석 선점 캐시 저장에 실패했습니다.");
        }

        // 홀드 성공 Response DTO 반환 (홀드 토큰, 만료 시각 포함)
        return PublicSeatHoldResponseDto.builder()
                .roundId(round.getRoundId())
                .seatIds(seatIds)
                .holdToken(holdToken)
                .holdExpiresAt(holdExpiresAt)
                .build();
    }

    // 5. 좌석 ID 리스트에서 중복 제거 및 유효성 검사 (null 또는 빈 리스트 허용하지 않음)
    private List<Long> normalizeSeatIds(List<Long> seatIds) {

        // seatIds가 null 또는 빈 리스트인지 확인
        if (seatIds == null || seatIds.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "seatIds는 최소 1개 이상이어야 합니다.");
        }

        // seatIds에서 null 값이 있는지 확인하고, 중복 제거하여 seat_id List 반환
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

    // 6. Redis에 저장할 좌석 홀드 키 생성 (예: "public-seat:hold:1:100" -> 라운드 ID 1, 좌석 ID 100)
    private String buildSeatHoldKey(Integer roundId, Long seatId) {
        return "public-seat:hold:" + roundId + ":" + seatId;
    }

    // 7. '선택 좌석 확인' 화면에서 이탈/뒤로가기 시, 본인 선점 좌석을 즉시 해제 (LOCKED -> AVAILABLE)
    @Transactional
    public void releaseHeldSeats(PublicSeatHoldReleaseRequestDto request, Authentication authentication) {

        // authentication 발급 여부 확인
        if (authentication == null || authentication.getName() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }

        // 인증 객체에서 user_id 추출 및 사용자 조회 (인증 정보가 없거나 사용자 정보가 존재하지 않으면 401 UNAUTHORIZED)
        String userId = authentication.getName();
        User user = userRepository.findUserByUserIdWithoutSoftDeleted(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "사용자 정보를 찾을 수 없습니다."));

        // 프론트 Request에서 seat_id List 추출 및 중복 제거/유효성 검사 (null 또는 빈 리스트 허용하지 않음)
        List<Long> seatIds = normalizeSeatIds(request.getSeatIds());

        // round_id에 해당하는 라운드 조회 (존재하지 않으면 404 NOT FOUND)
        PublicRound round = publicRoundRepository.findByRoundId(request.getRoundId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "현재 라운드를 찾을 수 없습니다."));

        // 본인 선점 좌석만 즉시 해제 (토큰까지 일치해야 해제 가능)
        publicSeatRepository.releaseLockedSeats(
                round.getId(),
                seatIds,
            user.getUserId(),
                request.getHoldToken()
        );

        // Redis 홀드 키도 함께 제거 (이미 만료/삭제된 키는 무시)
        for (Long seatId : seatIds) {
            String redisKey = buildSeatHoldKey(round.getRoundId(), seatId);
            stringRedisTemplate.delete(redisKey);
        }
    }
}
