package copy_ticket.copy_ticket.controller;

import copy_ticket.copy_ticket.dto.PublicQueueJoinRequestDto;
import copy_ticket.copy_ticket.dto.PublicQueueStatusResponseDto;
import copy_ticket.copy_ticket.service.PublicQueueService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public-queue")
@RequiredArgsConstructor
public class PublicQueueController {

    private final PublicQueueService publicQueueService;

    // 대기열 참가 API 엔드포인트 (인증된 사용자만 진입 가능)
    @PostMapping("/join")
    public ResponseEntity<PublicQueueStatusResponseDto> joinQueue(
            @Valid @RequestBody PublicQueueJoinRequestDto request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(publicQueueService.joinQueue(request.getRoundId(), authentication));
    }

    // 사용자의 현재 대기 상태 조회 API 엔드포인트 (인증된 사용자만 진입 가능)
    @GetMapping("/status")
    public ResponseEntity<PublicQueueStatusResponseDto> getQueueStatus(
            @RequestParam Integer roundId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(publicQueueService.getQueueStatus(roundId, authentication));
    }
}
