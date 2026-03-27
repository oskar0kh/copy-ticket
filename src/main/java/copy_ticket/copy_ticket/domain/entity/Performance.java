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

/**
 * 공연 정보 — API 응답에서 파싱해서 저장
 * 사용자당 최대 5개까지만 저장 가능 (soft delete로 관리)
 */
@Entity
@Table(name = "performances", indexes = {
        @Index(name = "idx_performances_source_url", columnList = "source_url"),
        @Index(name = "idx_performances_created_by", columnList = "created_by")
})
@Getter
@Setter
@NoArgsConstructor
public class Performance {

    // ========== 1. ID ==========
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ========== 2-14. 13개 핵심 필드 ==========
    @Column(name = "source_url", nullable = false, length = 2048)
    private String sourceUrl;

    @Column(name = "goods_code", length = 50)
    private String goodsCode;

    @Column(name = "goods_name", nullable = false, length = 500)
    private String goodsName;

    @Column(name = "sub_goods_name", length = 1000)
    private String subGoodsName;

    @Column(name = "place_name", length = 500)
    private String placeName;

    @Column(name = "view_rate_name", length = 100)
    private String viewRateName;

    @Column(name = "running_time", length = 20)
    private String runningTime;

    @Column(name = "play_start_date", length = 10)
    private String playStartDate;

    @Column(name = "play_end_date", length = 10)
    private String playEndDate;

    @Column(name = "goods_large_image_url", length = 2048)
    private String goodsLargeImageUrl;

    @Column(name = "ticket_open_date", length = 20)
    private String ticketOpenDate;

    @Column(name = "booking_end_date", length = 20)
    private String bookingEndDate;

    @Column(name = "ticket_cast_count")
    private Integer ticketCastCount;

    // ========== 14. 공연 순위 정보 ==========
    @Column(name = "week_rank", length = 10)
    private String weekRank;

    // ========== 15-18. 메타 필드 ==========
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
