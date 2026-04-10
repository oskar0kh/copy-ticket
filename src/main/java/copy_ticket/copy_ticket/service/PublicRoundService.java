package copy_ticket.copy_ticket.service;

import copy_ticket.copy_ticket.domain.entity.PublicRound;
import copy_ticket.copy_ticket.domain.entity.PublicRound.RoundStatus;
import copy_ticket.copy_ticket.repository.PublicRoundRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PublicRoundService {

    private final PublicRoundRepository publicRoundRepository;

    /**
     * 새로운 공개 라운드 생성
     * 매시 00분, 30분마다 호출됨
     */
    @Transactional
    public PublicRound createRound() {
        // 마지막 라운드 조회하여 roundNumber 증가
        int nextRoundNumber = publicRoundRepository.findAll().stream()
                .mapToInt(PublicRound::getRoundNumber)
                .max()
                .orElse(0) + 1;

        Instant now = Instant.now();
        PublicRound newRound = PublicRound.builder()
                .roundNumber(nextRoundNumber)
                .status(RoundStatus.OPEN)
                .openAt(now)
                .closeAt(now.plus(30, ChronoUnit.MINUTES))  // 30분 윈도우
                .createdAt(now)
                .updatedAt(now)
                .build();

        PublicRound saved = publicRoundRepository.save(newRound);
        log.info("New public round created: roundNumber={}, openAt={}", nextRoundNumber, now);
        return saved;
    }

    /**
     * 현재 OPEN 상태인 라운드 조회
     */
    @Transactional(readOnly = true)
    public Optional<PublicRound> getCurrentRound() {
        return publicRoundRepository.findByStatus(RoundStatus.OPEN);
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
}
