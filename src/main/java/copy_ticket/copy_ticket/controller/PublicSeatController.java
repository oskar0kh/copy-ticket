package copy_ticket.copy_ticket.controller;

import copy_ticket.copy_ticket.dto.PublicSeatHoldRequestDto;
import copy_ticket.copy_ticket.dto.PublicSeatHoldReleaseRequestDto;
import copy_ticket.copy_ticket.dto.PublicSeatHoldResponseDto;
import copy_ticket.copy_ticket.dto.PublicSeatResponseDto;
import jakarta.validation.Valid;
import copy_ticket.copy_ticket.service.PublicSeatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

    // 좌석 선택 창에서 '선택 완료' 버튼 눌렀을 때, 좌석 홀드 요청 처리/토큰 발급(AVAILABLE -> LOCKED) 및 '선택 좌석 확인' 화면 진입
    @PostMapping("/hold")
    public ResponseEntity<PublicSeatHoldResponseDto> holdSeats(
            @Valid @RequestBody PublicSeatHoldRequestDto request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(publicSeatService.holdSeats(request, authentication));
    }

    // '선택 좌석 확인' 화면에서 나갔을 때, 좌석 홀드 해제 Request 처리(LOCKED -> AVAILABLE, 토큰/만료 시각 삭제)
    @DeleteMapping("/hold")
    public ResponseEntity<Void> releaseHeldSeats(
            @Valid @RequestBody PublicSeatHoldReleaseRequestDto request,
            Authentication authentication
    ) {
        publicSeatService.releaseHeldSeats(request, authentication);
        return ResponseEntity.noContent().build();
    }
}
