package copy_ticket.copy_ticket.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

// 예매 확정 Request DTO : 프론트에서 예매 확정 API(`POST /public/bookings/confirm`) 호출 시 전달하는 데이터 구조 

@Getter
@Setter
@NoArgsConstructor
public class PublicBookingConfirmRequestDto {

    @NotNull(message = "roundId는 필수입니다.")
    private Integer roundId;

    @NotEmpty(message = "seatIds는 최소 1개 이상이어야 합니다.")
    private List<@NotNull(message = "seatId는 null일 수 없습니다.") Long> seatIds;

    @NotBlank(message = "holdToken은 필수입니다.")
    private String holdToken;
}