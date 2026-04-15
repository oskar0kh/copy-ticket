package copy_ticket.copy_ticket.controller;

import copy_ticket.copy_ticket.dto.PublicBookingConfirmRequestDto;
import copy_ticket.copy_ticket.dto.PublicBookingConfirmResponseDto;
import copy_ticket.copy_ticket.service.PublicBookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public-booking")
@RequiredArgsConstructor
public class PublicBookingController {

    private final PublicBookingService publicBookingService;

    // 예매 확정 API
    @PostMapping("/confirm")
    public ResponseEntity<PublicBookingConfirmResponseDto> confirmBooking(
            @Valid @RequestBody PublicBookingConfirmRequestDto request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(publicBookingService.confirmBooking(request, authentication));
    }
}