package copy_ticket.copy_ticket.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PerformanceDto {

    // 기본 정보
    private String title;                        // 공연 제목
    private String description;                  // 공연 설명 (선택적)
    private String posterImageUrl;               // 포스터 이미지 URL (선택적)

    // 공연 정보
    private String venue;                        // 공연장 정보 (예: "서울 예술의전당 콘서트홀")
    private List<PerformanceSchedule> schedules; // 공연 일정 (공연 시작 시간, 런타임 정보 - 콘서트 일정이 여러 날짜일때 하루 단위로 묶기)
    private String priceRange;                   // 가격 정보 (예: "R석 100,000원 / S석 80,000원")

    // 예약 정보
    private LocalDateTime reservationOpenAt;     // 예매 오픈 시간
    private String reservationUrl;               // 예매 URL (선택적)

    // 메타 정보
    private String sourceUrl;                    // 원본 URL (예: 인터파크 티켓 페이지 URL)
    private LocalDateTime parsedAt;              // 데이터가 파싱된 시간

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PerformanceSchedule { // 공연이 여러 날짜에 걸쳐 있을 때, 하루 단위로 묶어서 표현하기 위한 클래스
        private LocalDateTime startDateTime;  // 공연 시작 시간
        private String runtimeMinutes;        // 공연 런타임 (예: "120분") - 인터파크 페이지에서 추출 가능하면 추출, 없으면 null
    }
}
