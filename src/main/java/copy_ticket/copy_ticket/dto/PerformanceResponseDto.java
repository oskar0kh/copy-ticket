package copy_ticket.copy_ticket.dto;

import copy_ticket.copy_ticket.domain.entity.Performance;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * GET /api/performance/parse
 * 인터파크 URL 파싱 후 응답하는 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PerformanceResponseDto {

    // ========== 12개 핵심 필드 ==========
    // 1. 원본 URL
    private String sourceUrl;

    // 2-9. 공연 정보
    private String goodsName;                   // 공연 제목
    private String subGoodsName;                // 공연 부제목
    private String placeName;                   // 공연 장소
    private String viewRateName;                // 관람 연령 (전체관람가, 12세이상 등)
    private String runningTime;                 // 공연 시간 (분 단위)
    private String playStartDate;               // 공연 시작일 (YYYY.MM.DD)
    private String playEndDate;                 // 공연 종료일 (YYYY.MM.DD)
    private String goodsLargeImageUrl;          // 공연 포스터 이미지 URL

    // 10-12. 예매 정보
    private String ticketOpenDate;              // 티켓 오픈 날짜 (YYYYMMDDHHmm)
    private String bookingEndDate;              // 예매 종료 날짜 (YYYYMMDDHHmm)
    private Integer ticketCastCount;            // 티켓캐스트 개수

    // 공연 순위 정보
    private String weekRank;                    // 주간 순위

    // 메타 정보
    private LocalDateTime parsedAt;             // 파싱 시각

    /**
     * Performance 엔티티를 PerformanceResponseDto로 변환
     */
    public static PerformanceResponseDto fromEntity(Performance performance) {
        return PerformanceResponseDto.builder()
                .sourceUrl(performance.getSourceUrl())
                .goodsName(performance.getGoodsName())
                .subGoodsName(performance.getSubGoodsName())
                .placeName(performance.getPlaceName())
                .viewRateName(performance.getViewRateName())
                .runningTime(performance.getRunningTime())
                .playStartDate(performance.getPlayStartDate())
                .playEndDate(performance.getPlayEndDate())
                .goodsLargeImageUrl(performance.getGoodsLargeImageUrl())
                .ticketOpenDate(performance.getTicketOpenDate())
                .bookingEndDate(performance.getBookingEndDate())
                .ticketCastCount(performance.getTicketCastCount())
                .weekRank(performance.getWeekRank())
                .parsedAt(LocalDateTime.ofInstant(performance.getCreatedAt(), ZoneId.of("UTC")))
                .build();
    }
}
