package copy_ticket.copy_ticket.service;

import copy_ticket.copy_ticket.domain.entity.User;
import copy_ticket.copy_ticket.dto.PublicQueueStatusResponseDto;
import copy_ticket.copy_ticket.repository.PublicRoundRepository;
import copy_ticket.copy_ticket.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PublicQueueService {

    // Redis 키 TTL : READY 상태일때 발급하는 입장 토큰의 유효시간 (5분)
    private static final Duration ENTRY_TOKEN_TTL = Duration.ofMinutes(5);

    // 활성 사용자 정보 : READY 상태로 진입한 사용자의 정보를 일정 시간 동안 보관 (예: 2초) -> 스케줄러가 정상적으로 토큰을 발급했는지 확인하는 용도
    private static final Duration ACTIVE_USER_TTL = Duration.ofSeconds(2);

    // 스케줄러 락 TTL : 스케줄러가 얻는 분산락의 TTL 시간 (대기열에서 다음 사용자를 READY 상태로 진입시킬 때 다른 스케줄러가 동시에 작업하지 않도록 하는 락의 TTL)
    private static final Duration SCHEDULER_LOCK_TTL = Duration.ofSeconds(5);

    // 스케줄러 분산 락 키 (Redis에 저장되는 키 이름) - 여러 스케줄러 인스턴스가 있을 때 동시에 대기열 작업을 수행하지 않도록 하는 락 키
    private static final String SCHEDULER_LOCK_KEY = "public-queue:scheduler-lock";

    private final PublicRoundRepository publicRoundRepository;
    private final UserRepository userRepository;
    private final StringRedisTemplate stringRedisTemplate;

    // 스케줄러 인스턴스 식별자 (분산 락에서 자신이 락을 획득했는지 확인하는 용도)
    private final String schedulerInstanceId = UUID.randomUUID().toString();

    // 1. 대기열 참가 메서드 : 사용자가 대기열에 참가할 때 호출 -> 이미 토큰이 있으면 READY 상태 반환, 없으면 대기열에 참가시키고 WAITING 상태 반환
    public PublicQueueStatusResponseDto joinQueue(Integer roundId, org.springframework.security.core.Authentication authentication) {
        
        // 사용자 인증, roundId 유효성 검사, 라운드 예매 가능 여부 검증
        User user = resolveAuthenticatedUser(authentication);
        validateRoundId(roundId);
        ensureRoundIsBookable(roundId);

        // 이미 토큰이 발급되어 있는지 확인(이미 READY 상태인지 확인)
        String tokenKey = buildTokenKey(roundId, user.getUserId());
        String existingToken = stringRedisTemplate.opsForValue().get(tokenKey);

        // 기존 토큰이 존재하면, 해당 토큰 반환 -> READY 상태로 응답
        if (existingToken != null && !existingToken.isBlank()) {
            Instant expiresAt = resolveTokenExpiryInstant(tokenKey);
            return PublicQueueStatusResponseDto.ready(roundId, existingToken, expiresAt);
        }

        // 만약 토큰이 없다면, 대기열에 참가 처리 (Set과 List 모두에 사용자 ID 추가)
        String queueSetKey = buildQueueSetKey(roundId); // 대기열 중복 참가 방지용
        String queueListKey = buildQueueListKey(roundId); // 대기열 순서 관리용

        // Set에 사용자 ID 추가 (중복 참가 방지) -> Set에 추가된 경우에만 List에도 추가하여 순서 관리
        Long added = stringRedisTemplate.opsForSet().add(queueSetKey, user.getUserId());
        if (added != null && added > 0) {
            stringRedisTemplate.opsForList().rightPush(queueListKey, user.getUserId());
        }

        // 대기열에서 자신의 위치 조회 -> WAITING 상태로 응답
        Integer position = findQueuePosition(queueListKey, user.getUserId());
        if (position == null) {
            // 비정상 상태 복구: set에는 있는데 list에 없는 경우 재삽입
            stringRedisTemplate.opsForList().rightPush(queueListKey, user.getUserId());
            position = findQueuePosition(queueListKey, user.getUserId());
        }

        return PublicQueueStatusResponseDto.waiting(roundId, position);
    }

    // 2. 대기열 상태 조회 메서드 : 사용자가 자신의 대기열 상태를 조회할 때 호출 -> 대기열 상태(WAITING, READY, CLOSED, NOT_IN_QUEUE 등) 반환
    public PublicQueueStatusResponseDto getQueueStatus(Integer roundId, org.springframework.security.core.Authentication authentication) {
        User user = resolveAuthenticatedUser(authentication);
        validateRoundId(roundId);

        // 현재 라운드가 예매 가능한 상태인지 확인 -> 예매 불가능한 상태면 대기열에서 제거하고 CLOSED 상태 반환
        if (!isRoundBookable(roundId)) {
            clearUserQueueArtifacts(roundId, user.getUserId());
            return PublicQueueStatusResponseDto.closed(roundId);
        }

        // READY 상태인지 확인 (토큰 존재 여부) -> READY 상태면 토큰과 만료 시간 반환
        String tokenKey = buildTokenKey(roundId, user.getUserId());
        String token = stringRedisTemplate.opsForValue().get(tokenKey);
        if (token != null && !token.isBlank()) {
            Instant expiresAt = resolveTokenExpiryInstant(tokenKey);
            return PublicQueueStatusResponseDto.ready(roundId, token, expiresAt);
        }

        // WAITING 상태인지 확인 (대기열에 있는 경우) -> WAITING 상태면 대기열 List상 자신의 위치 반환
        Integer position = findQueuePosition(buildQueueListKey(roundId), user.getUserId());
        if (position != null) {
            return PublicQueueStatusResponseDto.waiting(roundId, position);
        }

        // 대기열에 없는 경우 -> NOT_IN_QUEUE 상태 반환
        return PublicQueueStatusResponseDto.notInQueue(roundId);
    }

    // 3. 스케줄러 메서드 : 주기적으로 실행되며, 대기열에서 다음 사용자를 선택하여 READY 상태로 진입시키는 역할 (분산 락 이용하여 중복 실행 방지)
    public void grantEntryToNextUser() {

        // 스케줄러 중복 실행 방지 -> Redis에 락을 걸어서 다른 스케줄러 인스턴스가 동시에 작업하지 않도록 함 -> 락 획득에 실패하면 바로 종료
        if (!acquireSchedulerLock()) {
            return;
        }

        try {
            // 현재 예매 가능한 라운드 조회 -> 대기열에서 다음 사용자 선택 -> READY 상태로 진입시키기 (토큰 발급, Redis 데이터 관리)
            publicRoundRepository.findOneBookableOpenRound().ifPresent(round -> {
                
                // 현재 예매 가능한 라운드의 ID로 Redis 키 생성
                Integer roundId = round.getRoundId();
                String queueListKey = buildQueueListKey(roundId);
                String queueSetKey = buildQueueSetKey(roundId);
                String activeUserKey = buildActiveUserKey(roundId);

                while (true) {
                    // 대기열에서 다음 사용자 선택 (List에서 왼쪽부터 사용자 ID 가져오기) -> Set에서 해당 사용자 ID 제거 (중복 방지) -> 토큰 발급 및 Redis에 저장 -> 활성 사용자 정보 저장
                    String nextUserId = stringRedisTemplate.opsForList().leftPop(queueListKey);
                    if (nextUserId == null || nextUserId.isBlank()) {
                        return;
                    }

                    // Set에서 해당 사용자 ID 제거 (중복 방지) -> Set에서 제거된 경우에만 토큰 발급 및 활성 사용자 정보 저장, 그렇지 않으면 이미 다른 스케줄러가 처리한 사용자이므로 다음 사용자로 넘어감
                    Long removed = stringRedisTemplate.opsForSet().remove(queueSetKey, nextUserId);
                    if (removed == null || removed == 0) {
                        continue;
                    }

                    // 토큰 발급 및 Redis에 저장 -> 활성 사용자 정보 저장
                    String sessionToken = UUID.randomUUID().toString();
                    stringRedisTemplate.opsForValue().set(
                            buildTokenKey(roundId, nextUserId),
                            sessionToken,
                            ENTRY_TOKEN_TTL
                    );
                    stringRedisTemplate.opsForValue().set(activeUserKey, nextUserId, ACTIVE_USER_TTL);

                    log.info("PublicQueueScheduler: granted entry token. roundId={}, userId={}", roundId, nextUserId);
                    return;
                }
            });
        } finally {
            // 작업이 끝난 후에는 반드시 락 해제 -> 락을 획득한 인스턴스만 락을 해제하도록 구현
            releaseSchedulerLock();
        }
    }

    // --- 헬퍼 메서드들 ---
    private User resolveAuthenticatedUser(org.springframework.security.core.Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }

        return userRepository.findUserByUserIdWithoutSoftDeleted(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "사용자 정보를 찾을 수 없습니다."));
    }

    private void validateRoundId(Integer roundId) {
        if (roundId == null || roundId < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "roundId는 1 이상의 값이어야 합니다.");
        }
    }

    private void ensureRoundIsBookable(Integer roundId) {
        if (!isRoundBookable(roundId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "현재 예매 가능한 라운드가 아닙니다.");
        }
    }

    private boolean isRoundBookable(Integer roundId) {
        return publicRoundRepository.findOneBookableOpenRoundById(roundId).isPresent();
    }

    // 토큰의 남은 TTL을 조회하여 만료 시간 계산 -> 토큰이 없거나 TTL이 유효하지 않으면 null 반환
    private Instant resolveTokenExpiryInstant(String tokenKey) {
        Long ttlSeconds = stringRedisTemplate.getExpire(tokenKey);
        if (ttlSeconds == null || ttlSeconds < 0) {
            return null;
        }
        return Instant.now().plusSeconds(ttlSeconds);
    }

    // 4. 대기열에서 자신의 위치 조회 -> List에서 사용자 ID의 인덱스를 찾아서 위치 반환 (인덱스 + 1) -> 대기열에 없으면 null 반환
    private Integer findQueuePosition(String queueListKey, String userId) {
        List<String> queueUsers = stringRedisTemplate.opsForList().range(queueListKey, 0, -1);
        if (queueUsers == null || queueUsers.isEmpty()) {
            return null;
        }

        for (int index = 0; index < queueUsers.size(); index++) {
            if (userId.equals(queueUsers.get(index))) {
                return index + 1;
            }
        }
        return null;
    }

    // 5. 대기열에서 사용자 제거 (예매 불가능 상태이거나, READY 상태로 진입한 사용자가 일정 시간 내에 예매를 완료하지 못한 경우 등) -> 대기열에서 제거하고 관련 Redis 데이터 삭제
    private void clearUserQueueArtifacts(Integer roundId, String userId) {
        stringRedisTemplate.delete(buildTokenKey(roundId, userId));
        stringRedisTemplate.opsForSet().remove(buildQueueSetKey(roundId), userId);
        stringRedisTemplate.opsForList().remove(buildQueueListKey(roundId), 0, userId);
    }

    // 6. 스케줄러 락 획득 메서드 : Redis의 setIfAbsent 명령어로 락 획득 시도 -> 획득 성공하면 true, 실패하면 false 반환
    private boolean acquireSchedulerLock() {
        Boolean acquired = stringRedisTemplate.opsForValue().setIfAbsent(
                SCHEDULER_LOCK_KEY,
                schedulerInstanceId,
                SCHEDULER_LOCK_TTL
        );
        return Boolean.TRUE.equals(acquired);
    }

    // 7. 스케줄러 락 해제 메서드 : 락을 획득한 인스턴스만 락을 해제하도록 구현 -> 현재 락의 보유자가 자신인지 확인 후 삭제
    private void releaseSchedulerLock() {
        String holder = stringRedisTemplate.opsForValue().get(SCHEDULER_LOCK_KEY);
        if (schedulerInstanceId.equals(holder)) {
            stringRedisTemplate.delete(SCHEDULER_LOCK_KEY);
        }
    }

    // 8. Redis 키 생성 헬퍼 메서드들 -> 각 기능별로 Redis에 저장되는 데이터의 키를 일관된 형식으로 생성하기 위한 메서드들
    private String buildQueueListKey(Integer roundId) {
        return "public-queue:list:" + roundId;
    }

    private String buildQueueSetKey(Integer roundId) {
        return "public-queue:set:" + roundId;
    }

    private String buildTokenKey(Integer roundId, String userId) {
        return "public-queue:token:" + roundId + ":" + userId;
    }

    private String buildActiveUserKey(Integer roundId) {
        return "public-queue:active-user:" + roundId;
    }
}
