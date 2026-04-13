package copy_ticket.copy_ticket.controller;

import copy_ticket.copy_ticket.dto.PublicSeatResponseDto;
import copy_ticket.copy_ticket.service.PublicSeatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/public-seat")
@RequiredArgsConstructor
public class PublicSeatController {

    private final PublicSeatService publicSeatService;

    // 라운드 ID에 해당하는 좌석 정보 조회
    @GetMapping("/{roundId}")
    public ResponseEntity<List<PublicSeatResponseDto>> getSeatsByRoundId(@PathVariable Integer roundId) {
        return ResponseEntity.ok(publicSeatService.getSeatsByRoundId(roundId));
    }
}
