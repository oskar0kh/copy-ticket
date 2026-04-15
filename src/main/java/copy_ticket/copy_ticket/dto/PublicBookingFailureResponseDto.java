package copy_ticket.copy_ticket.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

// 예매 실패 Response DTO : PublicBookingService에서 예매 확정 실패 시, PublicBookingConfirmationException과 함께 반환하는 데이터 구조

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PublicBookingFailureResponseDto {

    private String message;
    private List<Long> failedSeatIds;
}