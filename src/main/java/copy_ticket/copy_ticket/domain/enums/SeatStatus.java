package copy_ticket.copy_ticket.domain.enums;

/**
 * 좌석 상태
 *  AVAILABLE — 선택 가능한 좌석
 *  LOCKED — 결제 페이지 진입 중 (Redis 락 + DB 선점). 5분 TTL 후 미결제면 AVAILABLE 전환
 *  BOOKED — 예매 완료
 */
public enum SeatStatus {
    AVAILABLE,
    LOCKED,
    BOOKED
}
