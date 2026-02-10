package copy_ticket.copy_ticket.domain.entity;

import copy_ticket.copy_ticket.domain.enums.TemplateCode;
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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDateTime;

/**
 * 공연 정보 — URL HTML 파싱 후 제목·이미지·설명·예매 오픈 시각 저장
 * reservation_open_at으로 해당 시간에 예매 버튼 활성화
 */
@Entity
@Table(name = "performances", indexes = {
        @Index(name = "idx_performances_reservation_open_at", columnList = "reservation_open_at"),
        @Index(name = "idx_performances_source_url", columnList = "source_url")
})
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Performance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_url", nullable = false, length = 2048)
    private String sourceUrl;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(name = "image_url", length = 2048)
    private String imageUrl;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "reservation_open_at")
    private LocalDateTime reservationOpenAt;

    @Column(name = "template_code", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private TemplateCode templateCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
