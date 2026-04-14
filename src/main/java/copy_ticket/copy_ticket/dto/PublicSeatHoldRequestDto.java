package copy_ticket.copy_ticket.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class PublicSeatHoldRequestDto {

    @NotNull(message = "roundId는 필수입니다.")
    private Integer roundId;

    @NotEmpty(message = "seatIds는 최소 1개 이상이어야 합니다.")
    private List<@NotNull(message = "seatId는 null일 수 없습니다.") Long> seatIds;
}
