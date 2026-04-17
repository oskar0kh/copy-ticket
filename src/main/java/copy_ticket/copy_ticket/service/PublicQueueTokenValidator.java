package copy_ticket.copy_ticket.service;

import copy_ticket.copy_ticket.domain.entity.User;
import copy_ticket.copy_ticket.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class PublicQueueTokenValidator {

    // HTTP 요청 헤더 이름 : 대기열 입장 토큰이 담기는 헤더 이름
    public static final String QUEUE_SESSION_TOKEN_HEADER = "X-Public-Queue-Token";

    private final StringRedisTemplate stringRedisTemplate;
    private final UserRepository userRepository;

    // 대기열 입장 토큰(READY) 검증 메서드 : 요청 헤더에서 토큰을 추출하여 Redis에 저장된 토큰과 비교하는 방식으로 유효성 검증 수행
    public void validate(Integer roundId, org.springframework.security.core.Authentication authentication, String queueSessionToken) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }

        if (roundId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "roundId는 필수입니다.");
        }

        if (queueSessionToken == null || queueSessionToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "대기열 입장 토큰이 필요합니다.");
        }

        User user = userRepository.findUserByUserIdWithoutSoftDeleted(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "사용자 정보를 찾을 수 없습니다."));

        String tokenKey = buildTokenKey(roundId, user.getUserId());
        String storedToken = stringRedisTemplate.opsForValue().get(tokenKey);

        if (storedToken == null || storedToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "대기열 입장 토큰이 만료되었거나 존재하지 않습니다.");
        }

        if (!Objects.equals(storedToken, queueSessionToken)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "대기열 입장 토큰이 유효하지 않습니다.");
        }
    }

    private String buildTokenKey(Integer roundId, String userId) {
        return "public-queue:token:" + roundId + ":" + userId;
    }
}
