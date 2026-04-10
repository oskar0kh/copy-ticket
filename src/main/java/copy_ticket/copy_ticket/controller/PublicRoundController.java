package copy_ticket.copy_ticket.controller;

import copy_ticket.copy_ticket.domain.entity.PublicRound;
import copy_ticket.copy_ticket.dto.RoundEventDto;
import copy_ticket.copy_ticket.service.PublicRoundService;
import copy_ticket.copy_ticket.service.RoundEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Instant;
import java.util.Optional;

@RestController
@RequestMapping("/api/public-round")
@RequiredArgsConstructor
@Slf4j
public class PublicRoundController {

    private final PublicRoundService publicRoundService;
    private final RoundEventPublisher roundEventPublisher;

    /**
     * SSE 구독 엔드포인트
     * 클라이언트가 라운드 생성 이벤트를 받기 위해 구독
     */
    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe() {
        log.info("New client subscribed to round events");
        return roundEventPublisher.subscribe();
    }

    /**
     * 현재 OPEN 상태인 라운드 조회
     */
    @GetMapping("/current")
    public ResponseEntity<?> getCurrentRound() {
        Optional<PublicRound> round = publicRoundService.getCurrentRound();

        if (round.isPresent()) {
            RoundEventDto dto = RoundEventDto.of(round.get());
            return ResponseEntity.ok(dto);
        }

        // OPEN 라운드가 없으면 404
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("No active round at the moment"));
    }

    /**
     * 초기 동기화 엔드포인트
     * 현재 서버 시각과 현재 라운드 정보를 함께 반환
     */
    @GetMapping("/sync")
    public ResponseEntity<SyncResponse> getSync() {
        Optional<PublicRound> round = publicRoundService.getCurrentRound();
        RoundEventDto dto = round.map(RoundEventDto::of).orElse(null);

        return ResponseEntity.ok(new SyncResponse(Instant.now(), dto));
    }

    /**
     * 헬스 체크 (관리자용)
     */
    @GetMapping("/health")
    public ResponseEntity<?> health() {
        Optional<PublicRound> round = publicRoundService.getCurrentRound();
        int subscriberCount = roundEventPublisher.getActiveSubscriberCount();

        return ResponseEntity.ok(new HealthResponse(
                round.isPresent() ? round.get().getRoundNumber() : null,
                subscriberCount
        ));
    }

    @lombok.Getter
    @lombok.AllArgsConstructor
    static class ErrorResponse {
        private String message;
    }

    @lombok.Getter
    @lombok.AllArgsConstructor
    static class HealthResponse {
        private Integer currentRoundNumber;
        private int activeSubscribers;
    }

    @lombok.Getter
    @lombok.AllArgsConstructor
    static class SyncResponse {
        private Instant serverNow;
        private RoundEventDto round;
    }
}
