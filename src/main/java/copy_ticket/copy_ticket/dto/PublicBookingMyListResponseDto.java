package copy_ticket.copy_ticket.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

// 예매내역 조회 API Response DTO : '나의 예매내역' 모달창에 보여줄 정보들 담는 DTO

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicBookingMyListResponseDto {

    private Integer roundId;
    private List<Long> seatIds;
    private List<String> seatNumbers;
    private int bookingCount;
    private Instant bookedAt;

    public static PublicBookingMyListResponseDto from(
            Integer roundId,
            List<Long> seatIds,
            List<String> seatNumbers,
            Instant bookedAt
    ) {
        return PublicBookingMyListResponseDto.builder()
                .roundId(roundId)
                .seatIds(seatIds)
                .seatNumbers(seatNumbers)
                .bookingCount(seatIds.size())
                .bookedAt(bookedAt)
                .build();
    }
}
