package copy_ticket.copy_ticket.service;

import copy_ticket.copy_ticket.domain.entity.PublicRound;
import copy_ticket.copy_ticket.dto.RoundEventDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
@Slf4j
public class RoundEventPublisher {

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    /**
     * 클라이언트 구독 등록
     */
    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(300000L);  // 5분 타임아웃

        // 연결 확인 (heartbeat)
        try {
            emitter.send(SseEmitter.event()
                    .id("heartbeat")
                    .name("init")
                    .data(new InitEventPayload("connected", Instant.now()))
                    .build());
        } catch (IOException e) {
            log.warn("Failed to send heartbeat", e);
            emitters.remove(emitter);
            return emitter;
        }

        // Emitter 등록
        emitters.add(emitter);
        log.info("Client subscribed to round events. Total emitters: {}", emitters.size());

        // Emitter 완료/에러 처리
        emitter.onCompletion(() -> {
            emitters.remove(emitter);
            log.info("Emitter completed. Remaining: {}", emitters.size());
        });
        emitter.onTimeout(() -> {
            emitters.remove(emitter);
            log.info("Emitter timeout. Remaining: {}", emitters.size());
        });
        emitter.onError(throwable -> {
            emitters.remove(emitter);
            log.warn("Emitter error: {}", throwable.getMessage());
        });

        return emitter;
    }

    /**
     * 모든 구독자에게 라운드 생성 이벤트 발행
     */
    public void publishRoundCreated(PublicRound round) {
        RoundEventDto eventDto = RoundEventDto.of(round);
        List<SseEmitter> failedEmitters = new ArrayList<>();

        for (SseEmitter emitter : emitters) {
            try {
                SseEmitter.SseEventBuilder event = SseEmitter.event()
                        .id(String.valueOf(System.currentTimeMillis()))
                        .name("roundCreated")
                        .data(eventDto)
                        .reconnectTime(1000);
                emitter.send(event.build());
                log.debug("Event sent to emitter: roundNumber={}", round.getRoundNumber());
            } catch (IOException e) {
                log.warn("Failed to send event to emitter", e);
                failedEmitters.add(emitter);
            }
        }

        // 실패한 Emitter 제거
        failedEmitters.forEach(emitters::remove);
        if (!failedEmitters.isEmpty()) {
            log.info("Removed {} failed emitters. Remaining: {}", failedEmitters.size(), emitters.size());
        }
    }

    /**
     * 활성 구독자 수 조회
     */
    public int getActiveSubscriberCount() {
        return emitters.size();
    }

    @lombok.Getter
    @lombok.AllArgsConstructor
    static class InitEventPayload {
        private String type;
        private Instant serverNow;
    }
}
