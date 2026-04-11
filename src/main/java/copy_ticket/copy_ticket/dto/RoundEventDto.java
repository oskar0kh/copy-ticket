package copy_ticket.copy_ticket.dto;

import copy_ticket.copy_ticket.domain.entity.PublicRound;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoundEventDto {

    private Long id;
    private Integer roundNumber;
    private String status;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private Instant openAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private Instant closeAt;

    // 서버 시각 (클라이언트와의 시간 동기화 용도)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private Instant serverNow;

    public static RoundEventDto of(PublicRound round) {
        return RoundEventDto.builder()
                .id(round.getId())
                .roundNumber(round.getRoundNumber())
                .status(round.getStatus().name())
                .openAt(round.getOpenAt())
                .closeAt(round.getCloseAt())
                .serverNow(Instant.now())
                .build();
    }
}
