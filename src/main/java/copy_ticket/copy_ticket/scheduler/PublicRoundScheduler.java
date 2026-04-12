package copy_ticket.copy_ticket.scheduler;

import copy_ticket.copy_ticket.service.PublicRoundService;
import copy_ticket.copy_ticket.service.RoundEventPublisher;
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
     * 매시 정각(00분)과 30분에 현재 슬롯 OPEN 라운드를 생성
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
                                log.info("PublicRoundScheduler: openRoundForSlot completed with roundId={}", round.getRoundId());
                            },
                            () -> log.info("PublicRoundScheduler: openRoundForSlot completed without opening a round")
                    );
            log.info("=== PublicRoundScheduler: Open flow done ===");
        } catch (Exception e) {
            log.error("Error opening current slot round", e);
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
}
