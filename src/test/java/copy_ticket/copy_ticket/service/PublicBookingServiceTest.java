package copy_ticket.copy_ticket.service;

import copy_ticket.copy_ticket.domain.entity.PublicRound;
import copy_ticket.copy_ticket.domain.entity.PublicSeat;
import copy_ticket.copy_ticket.domain.entity.User;
import copy_ticket.copy_ticket.domain.enums.SeatStatus;
import copy_ticket.copy_ticket.dto.PublicBookingConfirmRequestDto;
import copy_ticket.copy_ticket.dto.PublicBookingConfirmResponseDto;
import copy_ticket.copy_ticket.exception.PublicBookingConfirmationException;
import copy_ticket.copy_ticket.repository.PublicBookingRepository;
import copy_ticket.copy_ticket.repository.PublicRoundRepository;
import copy_ticket.copy_ticket.repository.PublicSeatRepository;
import copy_ticket.copy_ticket.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublicBookingServiceTest {

    @Mock
    private PublicRoundRepository publicRoundRepository;

    @Mock
    private PublicSeatRepository publicSeatRepository;

    @Mock
    private PublicBookingRepository publicBookingRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @InjectMocks
    private PublicBookingService publicBookingService;

    @Test
    void confirmBooking_shouldPersistBookingsAndDeleteRedisKeys() {
        Authentication authentication = new UsernamePasswordAuthenticationToken("tester", "N/A");

                User user = User.createForSignup("tester", "password", "Tester");
                user.setId(1L);

        PublicRound round = PublicRound.builder()
                .id(10L)
                .roundId(1001)
                .status(PublicRound.RoundStatus.OPEN)
                .openAt(java.time.Instant.now().minusSeconds(60))
                .closeAt(java.time.Instant.now().plusSeconds(600))
                .createdAt(java.time.Instant.now())
                .updatedAt(java.time.Instant.now())
                .build();

        PublicSeat seat1 = PublicSeat.available(round, "S001");
        seat1.setId(101L);
        seat1.setRound(round);
        seat1.setStatus(SeatStatus.LOCKED);
        seat1.setLockedByUserId("tester");
        seat1.setHoldToken("hold-token");
        seat1.setHoldExpiresAt(java.time.Instant.now().plusSeconds(300));

        PublicSeat seat2 = PublicSeat.available(round, "S002");
        seat2.setId(102L);
        seat2.setRound(round);
        seat2.setStatus(SeatStatus.LOCKED);
        seat2.setLockedByUserId("tester");
        seat2.setHoldToken("hold-token");
        seat2.setHoldExpiresAt(java.time.Instant.now().plusSeconds(300));

        PublicBookingConfirmRequestDto request = new PublicBookingConfirmRequestDto();
        request.setRoundId(1001);
        request.setSeatIds(List.of(101L, 102L));
        request.setHoldToken("hold-token");

        when(userRepository.findUserByUserIdWithoutSoftDeleted("tester")).thenReturn(Optional.of(user));
        when(publicRoundRepository.findOneBookableOpenRoundById(1001)).thenReturn(Optional.of(round));
        when(publicSeatRepository.confirmBookedSeats(10L, List.of(101L, 102L), "tester", "hold-token"))
                .thenReturn(2);
        when(publicSeatRepository.findSeatIdsByRoundId(10L, List.of(101L, 102L))).thenReturn(List.of(seat1, seat2));
        when(publicBookingRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        PublicBookingConfirmResponseDto response = publicBookingService.confirmBooking(request, authentication);

        assertEquals(1001, response.getRoundId());
        assertEquals(List.of(101L, 102L), response.getSeatIds());
        assertEquals(2, response.getBookingCount());

        verify(publicBookingRepository).saveAll(anyList());
        verify(stringRedisTemplate).delete("public-seat:hold:1001:101");
        verify(stringRedisTemplate).delete("public-seat:hold:1001:102");
    }

    @Test
    void confirmBooking_shouldRejectInvalidSeatsAndRollbackBeforeSave() {
        Authentication authentication = new UsernamePasswordAuthenticationToken("tester", "N/A");

                User user = User.createForSignup("tester", "password", "Tester");

        PublicRound round = PublicRound.builder()
                .id(10L)
                .roundId(1001)
                .status(PublicRound.RoundStatus.OPEN)
                .openAt(java.time.Instant.now().minusSeconds(60))
                .closeAt(java.time.Instant.now().plusSeconds(600))
                .createdAt(java.time.Instant.now())
                .updatedAt(java.time.Instant.now())
                .build();

        PublicBookingConfirmRequestDto request = new PublicBookingConfirmRequestDto();
        request.setRoundId(1001);
        request.setSeatIds(List.of(101L, 102L));
        request.setHoldToken("hold-token");

        when(userRepository.findUserByUserIdWithoutSoftDeleted("tester")).thenReturn(Optional.of(user));
        when(publicRoundRepository.findOneBookableOpenRoundById(1001)).thenReturn(Optional.of(round));
        when(publicSeatRepository.confirmBookedSeats(10L, List.of(101L, 102L), "tester", "hold-token"))
                .thenReturn(0);
        when(publicSeatRepository.findSeatIdsByRoundId(10L, List.of(101L, 102L))).thenReturn(List.of(
                seat(round, 101L, SeatStatus.AVAILABLE, null, null, null),
                seat(round, 102L, SeatStatus.AVAILABLE, null, null, null)
        ));
        when(publicSeatRepository.findInvalidSeatIdsForBooking(10L, List.of(101L, 102L), "tester", "hold-token"))
                .thenReturn(List.of(101L));

        PublicBookingConfirmationException exception = assertThrows(
                PublicBookingConfirmationException.class,
                () -> publicBookingService.confirmBooking(request, authentication)
        );

        assertEquals(List.of(101L), exception.getFailedSeatIds());
        verify(publicBookingRepository, never()).saveAll(anyList());
                verify(publicSeatRepository).confirmBookedSeats(10L, List.of(101L, 102L), "tester", "hold-token");
    }

        private PublicSeat seat(PublicRound round, Long id, SeatStatus status, String lockedByUserId, String holdToken, java.time.Instant holdExpiresAt) {
                PublicSeat seat = PublicSeat.available(round, String.format("S%03d", id));
                seat.setId(id);
                seat.setRound(round);
                seat.setStatus(status);
                seat.setLockedByUserId(lockedByUserId);
                seat.setHoldToken(holdToken);
                seat.setHoldExpiresAt(holdExpiresAt);
                return seat;
        }
}