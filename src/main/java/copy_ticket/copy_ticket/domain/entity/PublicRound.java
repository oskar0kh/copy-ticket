package copy_ticket.copy_ticket.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * 공개 라운드 관리 (전역)
 * - 매시 10/40분 : 기존 라운드 CLOSED 처리, 새로운 라운드 생성 (WAITING 상태)
 * - 매시 00/30분 : WAITING 중인 라운드 → OPEN 상태로 변경
 * 모든 공연이 동일한 라운드를 공유
 */
@Entity
@Table(name = "public_rounds", indexes = {
        @Index(name = "idx_public_rounds_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PublicRound {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "round_number", nullable = false, unique = true)
    private Integer roundNumber;

    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private RoundStatus status;

    @Column(name = "open_at", nullable = false)
    private Instant openAt;

    @Column(name = "close_at", nullable = false)
    private Instant closeAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public enum RoundStatus {
        WAITING,   // 오픈 대기
        OPEN,      // 라운드 진행 중
        CLOSED     // 라운드 종료
    }
}
