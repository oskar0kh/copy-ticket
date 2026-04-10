package copy_ticket.copy_ticket.scheduler;

import copy_ticket.copy_ticket.domain.entity.PublicRound;
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
     * 매시 정각(00분)과 30분에 새로운 공개 라운드 생성
     * cron: "0 0,30 * * * *" = 매 시간 0분과 30분
     */
    @Scheduled(cron = "0 0,30 * * * *")
    public void createNewRound() {
        try {
            log.info("=== PublicRoundScheduler: Creating new round ===");

            // 1. 기존 OPEN 라운드가 있으면 CLOSED 처리 (PublicRoundService의 closeRound 메서드 사용)
            publicRoundService.getCurrentRound()
                    .ifPresent(publicRoundService::closeRound);

            // 2. 새로운 라운드 생성 (PublicRoundService의 createRound 메서드 사용)
            PublicRound newRound = publicRoundService.createRound();

            // 3. SSE로 모든 구독자에게 라운드 생성 이벤트 발행 (RoundEventPublisher의 publishRoundCreated 메서드 사용)
            roundEventPublisher.publishRoundCreated(newRound);

            log.info("=== PublicRoundScheduler: Round created successfully ===");
        } catch (Exception e) {
            log.error("Error creating new round", e);
        }
    }

    /**
     * 만료된 OPEN 라운드를 주기적으로 정리
        * cron: "0/5 * * * * *" = 5초마다 실행
     */
        @Scheduled(cron = "0/5 * * * * *")
    public void closeExpiredRound() {
        try {
            publicRoundService.getCurrentRound();
        } catch (Exception e) {
            log.error("Error while closing expired round", e);
        }
    }
}
