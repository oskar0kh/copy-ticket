package copy_ticket.copy_ticket.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * POST /api/performance/save
 * 파싱된 공연 정보를 DB에 저장하는 요청 DTO
 * (12개 핵심 필드만 포함)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PerformanceSaveRequestDto {

    // ========== 13개 핵심 필드 ==========
    private String sourceUrl;              // 1. 원본 URL (사용자가 입력한 URL)
    private String goodsName;              // 2. 공연 제목
    private String subGoodsName;           // 3. 공연 부제목
    private String placeName;              // 4. 공연 장소
    private String viewRateName;           // 5. 관람 연령 (전체관람가, 12세이상 등)
    private String runningTime;            // 6. 공연 시간 (분 단위)
    private String playStartDate;          // 7. 공연 시작일 (YYYY.MM.DD)
    private String playEndDate;            // 8. 공연 종료일 (YYYY.MM.DD)
    private String goodsLargeImageUrl;     // 9. 공연 포스터 이미지 URL
    private String ticketOpenDate;         // 10. 티켓 오픈 날짜 (YYYYMMDDHHmm)
    private String bookingEndDate;         // 11. 예매 종료 날짜 (YYYYMMDDHHmm)
    private Integer ticketCastCount;       // 12. 티켓캐스트 개수
    private String weekRank;               // 13. 콘서트 주간 순위
}
