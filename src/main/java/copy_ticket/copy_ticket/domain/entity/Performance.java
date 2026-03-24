package copy_ticket.copy_ticket.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDateTime;

/**
 * 공연 정보 — URL HTML 파싱 후 저장
 * 사용자당 최대 5개까지만 저장 가능 (soft delete로 관리)
 */
@Entity
@Table(name = "performances", indexes = {
        @Index(name = "idx_performances_reservation_open_at", columnList = "start_date"),
        @Index(name = "idx_performances_source_url", columnList = "source_url"),
        @Index(name = "idx_performances_goods_code", columnList = "goods_code")
})
@Getter
@Setter
@NoArgsConstructor
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

    @Column(name = "start_date", length = 12)
    private String startDate;

    @Column(name = "end_date", length = 12)
    private String endDate;

    @Column(length = 2048)
    private String link;

    @Column(name = "goods_code", unique = true, length = 50)
    private String goodsCode;

    @Column(name = "goods_name", length = 500)
    private String goodsName;

    @Column(name = "place_code", length = 50)
    private String placeCode;

    @Column(name = "place_name", length = 500)
    private String placeName;

    @Column(name = "play_date", length = 50)
    private String playDate;

    @Column(name = "play_start_date")
    private LocalDateTime playStartDate;

    @Column(name = "play_end_date")
    private LocalDateTime playEndDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;
}

