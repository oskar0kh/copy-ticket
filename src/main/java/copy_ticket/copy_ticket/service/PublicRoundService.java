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

    // 1. 매시 00/30분에 OPEN 라운드 생성
    @Transactional
    public Optional<PublicRound> openRoundForCurrentSlot() {
        
        // 만료된 OPEN 라운드 -> CLOSED로 전환
        closeExpiredOpenRounds();

        Instant now = Instant.now();

        /* 현재 시각 기준으로 이번 슬롯의 시작 시각 계산 (매시 00분 또는 30분)
         * 스케줄 경계(예: xx:29:59.9)에 아주 근접하게 실행될 때, openAt 시간이 이전 시간으로 계산되는 현상 방지
         *   ex: 14:30 슬롯 오픈 시점에 14:29:59.9로 인식되면, openAt 시간이 14:00로 계산될 수 있음
        */
        Instant currentSlotOpenAt = resolveCurrentSlotStart(now.plusSeconds(1));

        // 이미 OPEN 상태인 라운드가 이번 슬롯의 시작 시각과 일치하는지 확인
        Optional<PublicRound> existingOpenRound = publicRoundRepository.findOneOpenRound(RoundStatus.OPEN.name())
                .filter(round -> round.getOpenAt().equals(currentSlotOpenAt));
        
        // 이번 슬롯에 이미 OPEN 상태인 라운드가 존재하면 새로 OPEN하지 않고 빈 Optional 반환
        if (existingOpenRound.isPresent()) {
            return Optional.empty();
        }

        PublicRound openedRound = createOpenRoundForSlot(currentSlotOpenAt, now);
        log.info("Round created/opened for slot: roundNumber={}, openAt={}, closeAt={}", openedRound.getRoundNumber(), openedRound.getOpenAt(), openedRound.getCloseAt());
        return Optional.of(openedRound);
    }

    // 2. 만료된 OPEN 라운드를 CLOSED로 전환
    @Transactional
    public void closeExpiredOpenRounds() {
        Optional<PublicRound> openRound = publicRoundRepository.findOneOpenRound(RoundStatus.OPEN.name());
        if (openRound.isEmpty()) {
            return;
        }

        PublicRound round = openRound.get();
        if (!round.getCloseAt().isAfter(Instant.now())) {
            closeRound(round);
        }
    }

    /**
     * 3. 현재 OPEN 상태인 라운드 조회 (openAt <= now < closeAt 조건 포함)
     */
    @Transactional(readOnly = true)
    public Optional<PublicRound> getCurrentRound() {
        return publicRoundRepository.findOneBookableOpenRound(Instant.now());
    }

    /**
     * 4. 라운드 상태를 CLOSED로 업데이트
     */
    @Transactional
    public void closeRound(PublicRound round) {
        round.setStatus(RoundStatus.CLOSED);
        round.setUpdatedAt(Instant.now());
        publicRoundRepository.save(round);
        log.info("Public round closed: roundNumber={}", round.getRoundNumber());
    }

    // 5. 현재 슬롯의 시작 시각 계산 (매시 00분 또는 30분)
    private Instant resolveCurrentSlotStart(Instant now) {
        ZonedDateTime zonedNow = ZonedDateTime.ofInstant(now, ZoneId.systemDefault());
        int slotMinute = zonedNow.getMinute() < 30 ? 0 : 30;
        return zonedNow
                .withMinute(slotMinute)
                .withSecond(0)
                .withNano(0)
                .toInstant();
    }

    // 6. 현재 OPEN 라운드가 없으면, 새로운 OPEN 라운드 생성
    private PublicRound createOpenRoundForSlot(Instant slotOpenAt, Instant now) {

        // 이번 슬롯에 OPEN 라운드가 이미 존재하는지 다시 한번 확인 (동시성 대비)
        int nextRoundNumber = publicRoundRepository.findAll().stream()
                .mapToInt(PublicRound::getRoundNumber)
                .max()
                .orElse(0) + 1;

        // 새 OPEN 라운드 생성
        PublicRound newRound = PublicRound.builder()
                .roundNumber(nextRoundNumber)
                .status(RoundStatus.OPEN)
                .openAt(slotOpenAt)
                .closeAt(slotOpenAt.plus(10, ChronoUnit.MINUTES))
                .createdAt(now)
                .updatedAt(now)
                .build();

        return publicRoundRepository.save(newRound);
    }
}
