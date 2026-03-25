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

    // ========== DB 저장 필드 (schema.sql과 일치) ==========
    // 메타 정보
    private String sourceUrl;                    // 원본 URL (사용자가 입력한 URL)
    private LocalDateTime parsedAt;              // 파싱 시각

    // 기본 공연 정보
    private String title;                        // 공연 제목
    private String imageUrl;                     // 포스터/이미지 URL

    // 예매 시간
    private String startDate;                    // 예매 시작 (YYYYMMDDHHMM)
    private String endDate;                      // 예매 종료 (YYYYMMDDHHMM)

    // 공연 링크
    private String link;                         // 공연 URL (인터파크)

    // 인터파크 상품 정보
    private String goodsCode;                    // 인터파크 상품 코드
    private String goodsName;                    // 인터파크 상품명

    // 공연장 정보
    private String placeCode;                    // 공연장 코드
    private String placeName;                    // 공연장명

    // 공연 일정
    private String playDate;                     // 공연 날짜 범위 (예: "26-06-07 ~ 26-06-07")
    private String playStartDate;                // 공연 시작일 (YYYY.MM.DD)
    private String playEndDate;                  // 공연 종료일 (YYYY.MM.DD)

    /**
     * Performance 엔티티를 PerformanceResponseDto로 변환
     */
    public static PerformanceResponseDto fromEntity(Performance performance) {
        return PerformanceResponseDto.builder()
                .sourceUrl(performance.getSourceUrl())
                .parsedAt(LocalDateTime.ofInstant(performance.getCreatedAt(), ZoneId.of("UTC")))
                .title(performance.getTitle())
                .imageUrl(performance.getImageUrl())
                .startDate(performance.getStartDate())
                .endDate(performance.getEndDate())
                .link(performance.getLink())
                .goodsCode(performance.getGoodsCode())
                .goodsName(performance.getGoodsName())
                .placeCode(performance.getPlaceCode())
                .placeName(performance.getPlaceName())
                .playDate(performance.getPlayDate())
                .playStartDate(performance.getPlayStartDate())
                .playEndDate(performance.getPlayEndDate())
                .build();
    }
}
