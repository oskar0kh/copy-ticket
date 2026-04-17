package copy_ticket.copy_ticket.scheduler;

import copy_ticket.copy_ticket.service.PublicQueueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PublicQueueScheduler {

    private final PublicQueueService publicQueueService;

    // 1초마다 실행되는 스케줄러 : 대기열에서 다음 사용자를 READY 상태로 진입시키는 작업을 트리거한다.
    // 실제 큐 처리 로직은 PublicQueueService가 담당한다.
    @Scheduled(cron = "0/1 * * * * *")
    public void grantNextQueueEntry() {
        try {
            publicQueueService.grantEntryToNextUser();
        } catch (Exception exception) {
            log.error("PublicQueueScheduler: failed to grant queue entry", exception);
        }
    }
}
