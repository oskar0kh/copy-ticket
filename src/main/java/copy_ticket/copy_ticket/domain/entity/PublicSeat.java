package copy_ticket.copy_ticket.domain.entity;

import copy_ticket.copy_ticket.config.time.KstDateTimeUtils;
import copy_ticket.copy_ticket.domain.enums.SeatStatus;
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
 * 좌석 — 좌석 선택 화면 목록/상태 표시
 * 결제 페이지 진입 시 Redis 분산 락과 연동해 선점(LOCKED), 결제 완료 시 BOOKED
 */
@Entity
@Table(name = "public_seats",
        uniqueConstraints = @UniqueConstraint(name = "uq_public_seat_round_seat_number", columnNames = {"round_id", "seat_number"}),
        indexes = {
                @Index(name = "idx_public_seats_round_id", columnList = "round_id"),
                @Index(name = "idx_public_seats_status", columnList = "round_id, status"),
                @Index(name = "idx_public_seats_round_status_expires", columnList = "round_id, status, hold_expires_at"),
                @Index(name = "idx_public_seats_lock_owner_token", columnList = "locked_by_user_id, hold_token")
        })
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PublicSeat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "round_id", nullable = false)
    private PublicRound round;

    @Column(name = "seat_number", nullable = false, length = 20)
    private String seatNumber;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private SeatStatus status;

    @Column(name = "locked_at")
    private Instant lockedAt;

    @Column(name = "locked_by_user_id", length = 255)
    private String lockedByUserId;

    @Column(name = "hold_token", length = 120)
    private String holdToken;

    @Column(name = "hold_expires_at")
    private Instant holdExpiresAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    public static PublicSeat available(PublicRound round, String seatNumber) {
        PublicSeat seat = new PublicSeat();
        seat.setRound(round);
        seat.setSeatNumber(seatNumber);
        seat.setStatus(SeatStatus.AVAILABLE);
        seat.setUpdatedAt(KstDateTimeUtils.nowInstant());
        return seat;
    }
}
