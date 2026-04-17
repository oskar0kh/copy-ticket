package copy_ticket.copy_ticket.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

// 현재 사용자의 대기열 상태 반환용 Response DTO

@Getter
@Builder
public class PublicQueueStatusResponseDto {

    private Integer roundId;
    private String state;
    private Integer position;
    private Integer peopleAhead;
    private String sessionToken;
    private Instant tokenExpiresAt;

    public static PublicQueueStatusResponseDto waiting(Integer roundId, Integer position) {
        int safePosition = position == null || position < 1 ? 1 : position;
        return PublicQueueStatusResponseDto.builder()
                .roundId(roundId)
                .state("WAITING")
                .position(safePosition)
                .peopleAhead(Math.max(0, safePosition - 1))
                .build();
    }

    public static PublicQueueStatusResponseDto ready(Integer roundId, String token, Instant tokenExpiresAt) {
        return PublicQueueStatusResponseDto.builder()
                .roundId(roundId)
                .state("READY")
                .position(0)
                .peopleAhead(0)
                .sessionToken(token)
                .tokenExpiresAt(tokenExpiresAt)
                .build();
    }

    public static PublicQueueStatusResponseDto closed(Integer roundId) {
        return PublicQueueStatusResponseDto.builder()
                .roundId(roundId)
                .state("CLOSED")
                .build();
    }

    public static PublicQueueStatusResponseDto notInQueue(Integer roundId) {
        return PublicQueueStatusResponseDto.builder()
                .roundId(roundId)
                .state("NOT_IN_QUEUE")
                .build();
    }
}
