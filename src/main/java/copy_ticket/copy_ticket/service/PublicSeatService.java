package copy_ticket.copy_ticket.service;

import copy_ticket.copy_ticket.domain.entity.PublicRound;
import copy_ticket.copy_ticket.domain.entity.PublicSeat;
import copy_ticket.copy_ticket.dto.PublicSeatResponseDto;
import copy_ticket.copy_ticket.repository.PublicRoundRepository;
import copy_ticket.copy_ticket.repository.PublicSeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PublicSeatService {

    private final PublicRoundRepository publicRoundRepository;
    private final PublicSeatRepository publicSeatRepository;

    // 1. 라운드 ID에 해당하는 좌석 정보 조회
    @Transactional(readOnly = true)
    public List<PublicSeatResponseDto> getSeatsByRoundId(Integer roundId) {

        // 라운드 ID에 해당하는 라운드 조회 (존재하지 않으면 404 NOT FOUND)
        PublicRound round = publicRoundRepository.findByRoundId(roundId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "현재 라운드를 찾을 수 없습니다."));

        // 라운드에 해당하는 좌석 정보 조회 후, 'PublicSeat' 엔티티 list로 반환 (좌석 번호 오름차순 정렬)
        List<PublicSeat> seats = publicSeatRepository.findSeatNumberAscByRoundId(round.getId());

        // 'PublicSeat' 엔티티 list -> Response DTO로 변환
        return seats.stream()
                .map(this::toDto)
                .toList();
    }

    // 2. 'PublicSeat' 엔티티 -> PublicSeatResponseDto 변환
    private PublicSeatResponseDto toDto(PublicSeat seat) {

        // 좌석 번호에서 displayOrder 추출 (예: "S1" -> 1, "S20" -> 20, "A1" -> 0)
        int displayOrder = parseDisplayOrder(seat.getSeatNumber());
        return PublicSeatResponseDto.of(seat, displayOrder);
    }

    // 3. 좌석 번호에서 displayOrder 추출 (예: "S1" -> 1, "S20" -> 20, "A1" -> 0)
    private int parseDisplayOrder(String seatNumber) {
        
        // 좌석 번호가 "S"로 시작하지 않거나 숫자 부분이 없는 경우, displayOrder를 0으로 처리
        if (seatNumber == null || !seatNumber.startsWith("S")) {
            return 0;
        }

        // "S" 다음의 숫자 부분을 추출하여 displayOrder로 사용 (예: "S1" -> 1, "S20" -> 20)
        try {
            return Integer.parseInt(seatNumber.substring(1));
        } catch (NumberFormatException exception) {
            return 0;
        }
    }
}
