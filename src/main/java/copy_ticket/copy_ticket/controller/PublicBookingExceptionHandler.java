package copy_ticket.copy_ticket.controller;

import copy_ticket.copy_ticket.dto.PublicBookingFailureResponseDto;
import copy_ticket.copy_ticket.exception.PublicBookingConfirmationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class PublicBookingExceptionHandler {

    /*
    * 역할: PublicBookingService에서 발생하는 예외를 받아서, 클라이언트에게 에러 발생 원인 (Respones DTO - 409 에러 + LOCKED -> BOOKED 실패한 좌석 리스트) 반환

    * 1. 서비스에서 던진 커스텀 예외를 잡음
        * PublicBookingService에서 좌석 확정 실패 시 PublicBookingConfirmationException을 던지면, 이 클래스가 가로챔

    * 2. '예외 메시지(HTTP 409 CONFLICT 상태코드) + 실패한 좌석 ID 리스트'를 포함한 Response DTO를 생성하여 반환
        * Response DTO: PublicBookingFailureResponseDto (message + failedSeatIds)
    */
    @ExceptionHandler(PublicBookingConfirmationException.class)
    public ResponseEntity<PublicBookingFailureResponseDto> handlePublicBookingConfirmationException(PublicBookingConfirmationException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(PublicBookingFailureResponseDto.builder()
                        .message(exception.getMessage())
                        .failedSeatIds(exception.getFailedSeatIds())
                        .build());
    }
}