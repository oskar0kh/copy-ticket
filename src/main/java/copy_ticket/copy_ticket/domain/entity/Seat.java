package copy_ticket.copy_ticket.domain.entity;

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
@Table(name = "seats",
        uniqueConstraints = @UniqueConstraint(name = "uq_seat_performance_row_number", columnNames = {"performance_id", "row_name", "seat_number"}),
        indexes = {
                @Index(name = "idx_seats_performance_id", columnList = "performance_id"),
                @Index(name = "idx_seats_status", columnList = "performance_id, status")
        })
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Seat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performance_id", nullable = false)
    private Performance performance;

    @Column(length = 50)
    private String section;

    @Column(name = "row_name", nullable = false, length = 20)
    private String rowName;

    @Column(name = "seat_number", nullable = false, length = 20)
    private String seatNumber;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private SeatStatus status;

    @Column(name = "locked_at")
    private Instant lockedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "locked_by_user_id")
    private User lockedByUser;
}
