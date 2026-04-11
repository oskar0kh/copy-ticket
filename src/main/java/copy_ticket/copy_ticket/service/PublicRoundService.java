package copy_ticket.copy_ticket.service;

import copy_ticket.copy_ticket.domain.entity.PublicRound;
import copy_ticket.copy_ticket.domain.entity.PublicRound.RoundStatus;
import copy_ticket.copy_ticket.repository.PublicRoundRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PublicRoundService {

    private final PublicRoundRepository publicRoundRepository;

    // 새로운 라운드 생성 (WAITING 상태)
    @Transactional
    public PublicRound createWaitingRound(Instant openAt) {
        int nextRoundNumber = publicRoundRepository.findAll().stream()
                .mapToInt(PublicRound::getRoundNumber)
                .max()
                .orElse(0) + 1;

        Instant normalizedOpenAt = openAt.truncatedTo(ChronoUnit.MINUTES);
        Instant now = Instant.now();
        PublicRound newRound = PublicRound.builder()
                .roundNumber(nextRoundNumber)
                .status(RoundStatus.WAITING)
                .openAt(normalizedOpenAt)
                .closeAt(normalizedOpenAt.plus(10, ChronoUnit.MINUTES)) // OPEN 시각으로부터 10분 후에 자동으로 CLOSED 처리
                .createdAt(now)
                .updatedAt(now)
                .build();

        PublicRound saved = publicRoundRepository.save(newRound);
        log.info("Waiting round created: roundNumber={}, openAt={}", nextRoundNumber, normalizedOpenAt);
        return saved;
    }

    // 다음에 열릴 WAITING 라운드 조회 또는 생성
    @Transactional
    public PublicRound prepareNextWaitingRound() {
        Instant nextSlotOpenAt = resolveNextSlotStart(Instant.now());
        return publicRoundRepository.findWaitingRoundOpenAt(RoundStatus.WAITING.name(), nextSlotOpenAt)
                .orElseGet(() -> createWaitingRound(nextSlotOpenAt));
    }

    // 이번 슬롯(00/30분)에 해당하는 WAITING 라운드를 OPEN으로 전환
    @Transactional
    public Optional<PublicRound> openRoundForCurrentSlot() {
        closeExpiredOpenRounds(); // 먼저 만료된 OPEN 라운드 정리

        Instant now = Instant.now();
        Instant currentSlotOpenAt = resolveCurrentSlotStart(now); // 현재 시각 기준으로 이번 슬롯의 시작 시각 계산 (매시 00분 또는 30분)
        Optional<PublicRound> existingOpenRound = publicRoundRepository.findOpenRound(RoundStatus.OPEN.name()) // 이미 OPEN 상태인 라운드가 이번 슬롯의 시작 시각과 일치하는지 확인
                .filter(round -> round.getOpenAt().equals(currentSlotOpenAt));
        
        // 이번 슬롯에 이미 OPEN 상태인 라운드가 존재하면 새로 OPEN하지 않고 빈 Optional 반환
        if (existingOpenRound.isPresent()) {
            return Optional.empty();
        }

        // 이번 슬롯의 시작 시각에 맞는 WAITING 라운드를 조회하거나, 없으면 생성
        PublicRound waitingRound = publicRoundRepository
            .findWaitingRoundOpenAt(RoundStatus.WAITING.name(), currentSlotOpenAt)
                .orElseGet(() -> createWaitingRound(currentSlotOpenAt));

        // WAITING → OPEN으로 상태 변경
        waitingRound.setStatus(RoundStatus.OPEN);
        waitingRound.setUpdatedAt(now);

        // OPEN 상태로 저장
        PublicRound openedRound = publicRoundRepository.save(waitingRound);
        log.info("Round opened: roundNumber={}, openAt={}, closeAt={}", openedRound.getRoundNumber(), openedRound.getOpenAt(), openedRound.getCloseAt());
        return Optional.of(openedRound);
    }

    // 현재 시각 기준으로 이미 열렸어야 하는 WAITING 라운드를 OPEN으로 승격 (fallback 전용)
    @Transactional
    public Optional<PublicRound> promoteOverdueWaitingRound() {
        Instant now = Instant.now();
        return publicRoundRepository
            .findNotPromotedRound(now)
                .map(waitingRound -> {
                    waitingRound.setStatus(RoundStatus.OPEN);
                    waitingRound.setUpdatedAt(now);
                    PublicRound openedRound = publicRoundRepository.save(waitingRound);
                    log.info("Overdue waiting round promoted: roundNumber={}, openAt={}, closeAt={}", openedRound.getRoundNumber(), openedRound.getOpenAt(), openedRound.getCloseAt());
                    return openedRound;
                });
    }

    // 만료된 OPEN 라운드를 CLOSED로 전환
    @Transactional
    public void closeExpiredOpenRounds() {
        Optional<PublicRound> openRound = publicRoundRepository.findOpenRound(RoundStatus.OPEN.name());
        if (openRound.isEmpty()) {
            return;
        }

        PublicRound round = openRound.get();
        if (!round.getCloseAt().isAfter(Instant.now())) {
            closeRound(round);
        }
    }

    // OPEN/WATING 라운드가 모두 없을 때 현재 시각 기준 가장 가까운 다음 슬롯으로 WAITING 라운드를 생성
    @Transactional
    public Optional<PublicRound> ensureWaitingRoundWhenNoActiveRounds() {
        boolean hasOpenRound = publicRoundRepository.existsByStatus(RoundStatus.OPEN);
        boolean hasWaitingRound = publicRoundRepository.existsByStatus(RoundStatus.WAITING);

        if (hasOpenRound || hasWaitingRound) {
            return Optional.empty();
        }

        Instant fallbackOpenAt = resolveNearestUpcomingSlotStart(Instant.now());
        PublicRound createdRound = createWaitingRound(fallbackOpenAt);
        log.info("Fallback waiting round created: roundNumber={}, openAt={}", createdRound.getRoundNumber(), createdRound.getOpenAt());
        return Optional.of(createdRound);
    }

    /**
     * 현재 OPEN 상태인 라운드 조회
     * 기존 레포지토리 메서드 (getCurrentRound) -> findBookableOpenRound로 대체 (이유: 현재 시각 기준으로 OPEN인 라운드만 조회하기 위함)
     */
    @Transactional(readOnly = true)
    public Optional<PublicRound> getCurrentRound() {
        return publicRoundRepository.findBookableOpenRound(Instant.now());
    }

    /**
     * 라운드 상태를 CLOSED로 업데이트
     */
    @Transactional
    public void closeRound(PublicRound round) {
        round.setStatus(RoundStatus.CLOSED);
        round.setUpdatedAt(Instant.now());
        publicRoundRepository.save(round);
        log.info("Public round closed: roundNumber={}", round.getRoundNumber());
    }

    // 현재 슬롯의 시작 시각 계산 (매시 00분 또는 30분)
    private Instant resolveCurrentSlotStart(Instant now) {
        ZonedDateTime zonedNow = ZonedDateTime.ofInstant(now, ZoneId.systemDefault());
        int slotMinute = zonedNow.getMinute() < 30 ? 0 : 30;
        return zonedNow
                .withMinute(slotMinute)
                .withSecond(0)
                .withNano(0)
                .toInstant();
    }

    // 다음 슬롯의 시작 시각 계산 (현재 시각 기준으로 다음 00분 또는 30분)
    private Instant resolveNextSlotStart(Instant now) {
        return resolveCurrentSlotStart(now).plus(30, ChronoUnit.MINUTES);
    }

    // 현재 시각 기준으로 가장 가까운 다음 00분/30분 슬롯 시작 시각 계산
    private Instant resolveNearestUpcomingSlotStart(Instant now) {
        ZonedDateTime zonedNow = ZonedDateTime.ofInstant(now, ZoneId.systemDefault())
                .withSecond(0)
                .withNano(0);
        int minute = zonedNow.getMinute();

        if (minute == 0 || minute == 30) {
            return zonedNow.toInstant();
        }

        if (minute < 30) {
            return zonedNow.withMinute(30).toInstant();
        }

        return zonedNow.plusHours(1).withMinute(0).toInstant();
    }
}
