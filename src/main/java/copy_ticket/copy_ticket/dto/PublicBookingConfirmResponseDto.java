package copy_ticket.copy_ticket.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

// 예매 확정 Response DTO : 예매 확정 API(`POST /api/public-booking/confirm`) 호출 성공 시, 서버 -> 클라이언트에게 반환하는 데이터 구조

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PublicBookingConfirmResponseDto {

    private Integer roundId;
    private List<Long> seatIds;
    private Integer bookingCount;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private Instant bookedAt;
}