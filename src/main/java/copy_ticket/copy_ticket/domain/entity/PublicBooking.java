package copy_ticket.copy_ticket.domain.entity;

import copy_ticket.copy_ticket.domain.enums.BookingStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * 예매 내역 — 마이페이지용 + 예매 확정 확인용
 * "결제하기" 클릭 시 트랜잭션: 좌석 BOOKED, bookings에 COMPLETED 기록, Redis 락 해제
 */
@Entity
@Table(name = "public_bookings",
    uniqueConstraints = @UniqueConstraint(name = "uq_public_booking_round_seat", columnNames = {"round_id", "seat_id"}),
    indexes = @Index(name = "idx_public_bookings_user_created", columnList = "user_id, created_at"))
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PublicBooking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "round_id", nullable = false)
    private PublicRound round;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seat_id", nullable = false)
    private PublicSeat seat;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private BookingStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;
}
