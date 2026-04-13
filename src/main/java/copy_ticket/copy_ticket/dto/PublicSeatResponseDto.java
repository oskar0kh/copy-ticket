package copy_ticket.copy_ticket.dto;

import copy_ticket.copy_ticket.domain.entity.PublicSeat;
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
public class PublicSeatResponseDto {

    private Long id;
    private Long roundId;
    private String seatNumber;
    private Integer displayOrder;
    private String status;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private Instant lockedAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private Instant holdExpiresAt;

    public static PublicSeatResponseDto of(PublicSeat seat, int displayOrder) {
        return PublicSeatResponseDto.builder()
                .id(seat.getId())
                .roundId(seat.getRound() != null ? seat.getRound().getId() : null)
                .seatNumber(seat.getSeatNumber())
                .displayOrder(displayOrder)
                .status(seat.getStatus() != null ? seat.getStatus().name() : null)
                .lockedAt(seat.getLockedAt())
                .holdExpiresAt(seat.getHoldExpiresAt())
                .build();
    }
}
