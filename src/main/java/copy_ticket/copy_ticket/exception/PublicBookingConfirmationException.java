package copy_ticket.copy_ticket.exception;

import java.util.List;

// 예매 확정 실패 시 발생시키는 커스텀 예외 클래스
public class PublicBookingConfirmationException extends RuntimeException {

    private final List<Long> failedSeatIds;

    // 예매 확정 실패 시, 실패한 좌석 ID 리스트를 포함하여 예외를 생성
    public PublicBookingConfirmationException(String message, List<Long> failedSeatIds) {
        super(message);
        this.failedSeatIds = failedSeatIds;
    }

    // 실패한 좌석 ID 리스트 반환 메서드
    public List<Long> getFailedSeatIds() {
        return failedSeatIds;
    }
}