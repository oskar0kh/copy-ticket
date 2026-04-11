package copy_ticket.copy_ticket.scheduler;

import copy_ticket.copy_ticket.service.PublicRoundService;
import copy_ticket.copy_ticket.service.RoundEventPublisher;
import copy_ticket.copy_ticket.domain.entity.PublicRound;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PublicRoundScheduler {

    private final PublicRoundService publicRoundService;
    private final RoundEventPublisher roundEventPublisher;

    /**
     * 매시 정각(00분)과 30분에 WAITING 라운드를 OPEN으로 전환
     * cron: "0 0,30 * * * *" = 매 시간 0분과 30분
     */
    @Scheduled(cron = "0 0,30 * * * *")
    public void openRoundForSlot() {
        try {
            log.info("PublicRoundScheduler: openRoundForSlot started");
            log.info("=== PublicRoundScheduler: Opening current slot round ===");
            publicRoundService.openRoundForCurrentSlot()
                    .ifPresentOrElse(
                            round -> {
                                roundEventPublisher.publishRoundCreated(round); // OPEN된 라운드가 있으면 SSE 이벤트 발행
                                log.info("PublicRoundScheduler: openRoundForSlot completed with roundNumber={}", round.getRoundNumber());
                            },
                            () -> log.info("PublicRoundScheduler: openRoundForSlot completed without opening a round")
                    );
            log.info("=== PublicRoundScheduler: Open flow done ===");
        } catch (Exception e) {
            log.error("Error opening current slot round", e);
        }
    }

    /**
     * 매시 10분/40분에 다음 슬롯 WAITING 라운드를 미리 준비
     * cron: "0 10,40 * * * *" = 매 시간 10분과 40분
     */
    @Scheduled(cron = "0 10,40 * * * *")
    public void prepareNextWaitingRound() {
        try {
            log.info("PublicRoundScheduler: prepareNextWaitingRound started");
            PublicRound round = publicRoundService.prepareNextWaitingRound();
            log.info("PublicRoundScheduler: prepareNextWaitingRound completed with roundNumber={}, openAt={}", round.getRoundNumber(), round.getOpenAt());
        } catch (Exception e) {
            log.error("Error preparing next waiting round", e);
        }
    }

    /**
     * 만료된 OPEN 라운드를 주기적으로 정리
     * cron: "0/5 * * * * *" = 5초마다 실행
     */
    @Scheduled(cron = "0/5 * * * * *")
    public void closeExpiredRound() {
        try {
            log.info("PublicRoundScheduler: closeExpiredRound started");
            publicRoundService.closeExpiredOpenRounds();
            log.info("PublicRoundScheduler: closeExpiredRound completed");
        } catch (Exception e) {
            log.error("Error while closing expired round", e);
        }
    }

    /**
     * 60초마다 라운드 유실 상태를 점검하여 OPEN/WAITING가 모두 없으면 WAITING 라운드를 보정 생성
     */
    @Scheduled(fixedDelayString = "60000", initialDelayString = "0")
    public void ensureWaitingRoundFallback() {
        try {
            log.info("PublicRoundScheduler: ensureWaitingRoundFallback started");
            publicRoundService.promoteOverdueWaitingRound()
                    .ifPresentOrElse(
                            round -> {
                                roundEventPublisher.publishRoundCreated(round);
                                log.info("PublicRoundScheduler: promoted overdue waiting round roundNumber={}", round.getRoundNumber());
                            },
                            () -> log.info("PublicRoundScheduler: no overdue waiting round to promote")
                    );
            publicRoundService.ensureWaitingRoundWhenNoActiveRounds()
                    .ifPresentOrElse(
                            round -> log.info("PublicRoundScheduler: fallback waiting round created roundNumber={}, openAt={}", round.getRoundNumber(), round.getOpenAt()),
                            () -> log.info("PublicRoundScheduler: fallback waiting round not needed")
                    );
            log.info("PublicRoundScheduler: ensureWaitingRoundFallback completed");
        } catch (Exception e) {
            log.error("Error while running fallback round recovery", e);
        }
    }
}
