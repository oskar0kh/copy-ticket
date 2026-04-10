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
     * 매시 정각(00분)과 30분에 WAITING 라운드를 OPEN으로 전환
     * cron: "0 0,30 * * * *" = 매 시간 0분과 30분
     */
    @Scheduled(cron = "0 0,30 * * * *")
    public void openRoundForSlot() {
        try {
            log.info("=== PublicRoundScheduler: Opening current slot round ===");
            publicRoundService.openRoundForCurrentSlot()
                    .ifPresent(roundEventPublisher::publishRoundCreated); // OPEN된 라운드가 있으면 SSE 이벤트 발행
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
            publicRoundService.prepareNextWaitingRound();
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
            publicRoundService.closeExpiredOpenRounds();
        } catch (Exception e) {
            log.error("Error while closing expired round", e);
        }
    }

    /**
     * 1분마다 라운드 유실 상태를 점검하여 OPEN/WAITING가 모두 없으면 WAITING 라운드를 보정 생성
     * cron: "0 * * * * *" = 매 분 0초마다 실행
     */
    @Scheduled(cron = "0 * * * * *")
    public void ensureWaitingRoundFallback() {
        try {
            publicRoundService.promoteOverdueWaitingRound()
                    .ifPresent(roundEventPublisher::publishRoundCreated);
            publicRoundService.ensureWaitingRoundWhenNoActiveRounds();
        } catch (Exception e) {
            log.error("Error while running fallback round recovery", e);
        }
    }
}
