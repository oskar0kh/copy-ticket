package copy_ticket.copy_ticket.controller;

import copy_ticket.copy_ticket.dto.PublicBookingConfirmRequestDto;
import copy_ticket.copy_ticket.dto.PublicBookingConfirmResponseDto;
import copy_ticket.copy_ticket.dto.PublicBookingMyListResponseDto;
import copy_ticket.copy_ticket.service.PublicQueueTokenValidator;
import copy_ticket.copy_ticket.service.PublicBookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public-booking")
@RequiredArgsConstructor
public class PublicBookingController {

    private final PublicBookingService publicBookingService;
    private final PublicQueueTokenValidator publicQueueTokenValidator;

    // 예매 확정 API
    @PostMapping("/confirm")
    public ResponseEntity<PublicBookingConfirmResponseDto> confirmBooking(
            @Valid @RequestBody PublicBookingConfirmRequestDto request,
            Authentication authentication,
            @RequestHeader(name = PublicQueueTokenValidator.QUEUE_SESSION_TOKEN_HEADER, required = false) String queueSessionToken
    ) {
        // 대기열 입장 토큰 검증 : 예매 확정 요청 시, 요청 헤더에 포함된 대기열 입장 토큰의 유효성을 검증하여 인증된 사용자이면서 READY 상태로 진입한 사용자만 예매 확정이 가능하도록 처리
        publicQueueTokenValidator.validate(request.getRoundId(), authentication, queueSessionToken);
        return ResponseEntity.ok(publicBookingService.confirmBooking(request, authentication));
    }

    // 나의 예매내역 조회 API
    @GetMapping("/my-bookings")
    public ResponseEntity<PublicBookingMyListResponseDto> getMyBookings(
            Authentication authentication
    ) {
        return ResponseEntity.ok(publicBookingService.getMyBookings(authentication));
    }
}