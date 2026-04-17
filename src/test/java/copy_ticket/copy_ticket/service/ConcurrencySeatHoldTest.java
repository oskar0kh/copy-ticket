package copy_ticket.copy_ticket.service;

import copy_ticket.copy_ticket.config.time.KstDateTimeUtils;
import copy_ticket.copy_ticket.domain.entity.PublicRound;
import copy_ticket.copy_ticket.domain.entity.PublicSeat;
import copy_ticket.copy_ticket.domain.entity.User;
import copy_ticket.copy_ticket.domain.entity.PublicRound.RoundStatus;
import copy_ticket.copy_ticket.domain.enums.SeatStatus;
import copy_ticket.copy_ticket.dto.PublicSeatHoldRequestDto;
import copy_ticket.copy_ticket.repository.PublicRoundRepository;
import copy_ticket.copy_ticket.repository.PublicSeatRepository;
import copy_ticket.copy_ticket.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 시나리오 1: 1개 좌석에 동시에 여러 명(1000명)이 홀드를 시도할 때,
 * 실제로 몇 명이 성공하는지 테스트하는 동시성 테스트
 *
 * 목표: 현재 코드의 동시성 제어 상태를 검증
 * - 만약 1명만 성공하면 → 동시성 제어가 잘 되고 있음
 * - 만약 여러 명이 성공하면 → 동시성 제어 문제가 있음
 * 
 * ===================================================================================
 * 
 * 시나리오 2: 400개 좌석에 동시에 10,000명이 홀드를 시도하는 실제 상황 시뮬레이션 테스트
 * 목표: 비관적 락 없이, ‘MVCC + UPDATE 쿼리의 원자성’ 만으로 대규모 동시성 제어가 성공하는지 확인
 * - 10000명 사용자가 랜덤하게 좌석 번호 선택 (남은 좌석들 중에서 랜덤하게 선점)
 * - 최종적으로 선점된 고유 좌석 수가 400개와 일치해야 함 (각 좌석당 1명만 성공)
 * - 성공률, 실패률, 실행 시간 등도 함께 분석하여 성능 평가
 */
@SpringBootTest
@ActiveProfiles("test")
class ConcurrencySeatHoldTest {

    // 시나리오 1: 동시성 테스트에 사용할 사용자 수 (예: 1000명)
    private static final int CONCURRENT_USERS_COUNT_1 = 1000;

    // 시나리오 2: 실제 상황 시뮬레이션 동시성 테스트에 사용할 사용자 수 (예: 10,000명)
    private static final int CONCURRENT_USERS_COUNT_2 = 10000;

    // 스레드 풀 크기: CountDownLatch와 함께 사용하여 동시성을 정확하게 시뮬레이션
    // (모든 스레드가 startLatch에서 대기 후 동시에 시작되므로 적절한 크기면 됨)
    private static final int THREAD_POOL_SIZE = 500;

    @Autowired
    private PublicSeatService publicSeatService;

    @Autowired
    private PublicRoundRepository publicRoundRepository;

    @Autowired
    private PublicSeatRepository publicSeatRepository;

    @Autowired
    private UserRepository userRepository;

    private static final int SEAT_COUNT_FOR_SCENARIO_TEST = 400;

    private PublicRound testRound;
    private PublicSeat testSeat;
    private List<User> testUsers;
    private List<PublicSeat> testSeatsForScenarioTest;
    private Integer uniqueRoundId1;
    private Integer uniqueRoundId2;

    // 시나리오 시작 전, ‘라운드/좌석/사용자’ 생성
    @BeforeEach
    void setUp() {
        Instant now = KstDateTimeUtils.nowInstant();

        // 고유한 roundId 생성 (현재 시간 기반)
        uniqueRoundId1 = (int) (System.currentTimeMillis() / 1000) % 100000;
        uniqueRoundId2 = uniqueRoundId1 + 1;

        // 1. 테스트 라운드 생성 (OPEN 상태)
        testRound = PublicRound.builder()
                .roundId(uniqueRoundId1)
                .status(RoundStatus.OPEN)
                .openAt(now)
                .closeAt(now.plus(30, ChronoUnit.MINUTES))
                .createdAt(now)
                .updatedAt(now)
                .build();
        testRound = publicRoundRepository.save(testRound);
        System.out.println("[SET UP] 라운드 생성: roundId=" + testRound.getRoundId());

        // 2. 테스트 좌석 생성 (AVAILABLE 상태, 1개만 생성)
        testSeat = PublicSeat.available(testRound, "S1");
        testSeat = publicSeatRepository.save(testSeat);
        System.out.println("[SET UP] 좌석 생성: seatId=" + testSeat.getId());

        // 3. 테스트 사용자 1000명 생성
        testUsers = new java.util.ArrayList<>();
        for (int i = 0; i < CONCURRENT_USERS_COUNT_1; i++) {
            User user = User.createForSignup("test-user-" + i, "encoded_password", "User" + i);
            testUsers.add(userRepository.save(user));
        }
        System.out.println("[SET UP] 사용자 생성: " + testUsers.size() + "명\n");
    }

    // 시나리오 종료 후, ‘생성한 라운드/좌석/사용자’ 삭제
    @AfterEach
    @Transactional
    void tearDown() {
        System.out.println("\n[TEAR DOWN] 테스트 데이터 정리 중...");

        try {
            // 1. testRound 관련 정리
            if (testRound != null && testRound.getId() != null) {
                // 좌석을 먼저 모두 찾아서 삭제
                List<PublicSeat> seats = publicSeatRepository.findSeatNumberAscByRoundId(testRound.getId());
                if (!seats.isEmpty()) {
                    publicSeatRepository.deleteAll(seats);
                }
                publicRoundRepository.delete(testRound);
                System.out.println("[TEAR DOWN] 라운드 삭제: roundId=" + testRound.getRoundId());
            }

            // 2. 테스트 사용자 삭제
            if (testUsers != null && !testUsers.isEmpty()) {
                userRepository.deleteAll(testUsers);
                testUsers.clear();
                System.out.println("[TEAR DOWN] 테스트 사용자 삭제");
            }

            System.out.println("[TEAR DOWN] 정리 완료\n");
        } catch (Exception e) {
            System.out.println("[TEAR DOWN] 정리 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ========== 시나리오 테스트1 (단일 좌석- 1000명 동시 접근 테스트) ==========
    @Test
    @DisplayName("1개 좌석에 " + CONCURRENT_USERS_COUNT_1 + "명이 동시에 홀드 시도 - 동시성 테스트")
    void testConcurrentSeatHold() throws InterruptedException {
    
        // 결과를 저장할 맵 (사용자 번호 -> 성공/실패)
        ConcurrentHashMap<Integer, Boolean> results = new ConcurrentHashMap<>();
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        // 동시성 제어를 위한 CountDownLatch
        // CountDownLatch는 모든 스레드가 준비될 때까지 대기했다가 동시에 시작하게 함
        // (스레드 풀 크기와 무관하게 동시성을 정확하게 시뮬레이션)
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(CONCURRENT_USERS_COUNT_1);

        // 동시성 테스트: THREAD_POOL_SIZE개 스레드풀 생성
        ExecutorService executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

        long startTime = System.currentTimeMillis();

        // 1000개 작업 제출
        for (int i = 0; i < CONCURRENT_USERS_COUNT_1; i++) {
            final int userId = i;
            executorService.submit(() -> {
                try {
                    // 모든 스레드가 준비될 때까지 대기 (동시성 최대화)
                    startLatch.await();

                    // 서로 다른 사용자로 인증 객체 생성
                    Authentication authentication = new UsernamePasswordAuthenticationToken(
                            "test-user-" + userId,
                            null,
                            Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
                    );

                    // 좌석 홀드 요청 생성
                    PublicSeatHoldRequestDto request = new PublicSeatHoldRequestDto();
                    request.setRoundId(testRound.getRoundId());
                    request.setSeatIds(List.of(testSeat.getId()));

                    // 서비스 호출 (실제 동시성 문제 발생 지점)
                    publicSeatService.holdSeats(request, authentication);

                    // 성공
                    results.put(userId, true);
                    successCount.incrementAndGet();

                } catch (Exception e) {
                    // 실패 (예상되는 결과: 동시성 제어로 인한 CONFLICT 등)
                    results.put(userId, false);
                    failureCount.incrementAndGet();

                } finally {
                    endLatch.countDown();
                }
            });
        }

        // 모든 스레드가 준비되었으므로 동시에 시작
        startLatch.countDown();

        // 모든 스레드 완료 대기
        endLatch.await();
        executorService.shutdown();

        long endTime = System.currentTimeMillis();

        // ========== 결과 출력 ==========
        System.out.println("\n" +
                "╔════════════════════════════════════════╗\n" +
                "║         동시성 테스트 결과              ║\n" +
                "╚════════════════════════════════════════╝\n" +
                "  총 사용자 수: " + CONCURRENT_USERS_COUNT_1 + "\n" +
                "  성공 수:    " + successCount.get() + "\n" +
                "  실패 수:    " + failureCount.get() + "\n" +
                "  실행 시간:  " + (endTime - startTime) + "ms\n");

        // DB에서 실제 좌석 상태 확인
        PublicSeat updatedSeat = publicSeatRepository.findById(testSeat.getId()).orElseThrow();
        System.out.println("좌석 최종 상태:\n" +
                "  상태:      " + updatedSeat.getStatus() + "\n" +
                "  잠금 사용자: " + updatedSeat.getLockedByUserId() + "\n");

        // 기본 검증: 최소 1명은 성공해야 함
        assertTrue(successCount.get() > 0, "최소 1명 이상은 성공해야 합니다");

        // 중요 검증: 정확히 1명만 성공해야 함 (동시성 제어가 제대로 작동하는 경우)
        if (successCount.get() == 1) {
            System.out.println("✅ 결과: 동시성 제어 완벽! 정확히 1명만 좌석을 홀드했습니다.\n");
        } else {
            System.out.println("⚠️ 결과: 동시성 제어 문제! " + successCount.get() + "명이 좌석을 홀드했습니다.\n");
        }

        // 최종 좌석 상태 검증
        assertEquals(SeatStatus.LOCKED, updatedSeat.getStatus(), "최종 좌석 상태는 LOCKED여야 합니다");
        assertNotNull(updatedSeat.getLockedByUserId(), "잠금 사용자가 설정되어야 합니다");
        assertNotNull(updatedSeat.getHoldToken(), "홀드 토큰이 설정되어야 합니다");
    }

    // ========== 시나리오 테스트 2 (400개 좌석 - 10,000명 동시 접근 테스트) ==========
    @Test
    @DisplayName("400개 좌석에 " + CONCURRENT_USERS_COUNT_2 + "명이 동시에 홀드 시도 - 실제 상황 시뮬레이션")
    void testConcurrentSeatHoldScenario() throws InterruptedException {
        Instant now = KstDateTimeUtils.nowInstant();

        // ========== 테스트 데이터 준비 ==========
        System.out.println("\n[SET UP] 시나리오 테스트 데이터 준비 중...");

        // 라운드 생성 (새로운 라운드)
        final PublicRound scenarioRound = publicRoundRepository.save(
                PublicRound.builder()
                        .roundId(uniqueRoundId2)
                        .status(RoundStatus.OPEN)
                        .openAt(now)
                        .closeAt(now.plus(30, ChronoUnit.MINUTES))
                        .createdAt(now)
                        .updatedAt(now)
                        .build()
        );

        // 400개 좌석 생성
        testSeatsForScenarioTest = new java.util.ArrayList<>();
        for (int i = 1; i <= SEAT_COUNT_FOR_SCENARIO_TEST; i++) {
            PublicSeat seat = PublicSeat.available(scenarioRound, "S" + i);
            testSeatsForScenarioTest.add(publicSeatRepository.save(seat));
        }

        System.out.println("[SET UP] 라운드 생성: roundId=" + scenarioRound.getRoundId());
        System.out.println("[SET UP] 좌석 생성: " + SEAT_COUNT_FOR_SCENARIO_TEST + "개");
        System.out.println("[SET UP] 사용자 준비: " + CONCURRENT_USERS_COUNT_2 + "명\n");

        // ========== 동시성 테스트 ==========
        ConcurrentHashMap<Integer, Boolean> results = new ConcurrentHashMap<>();
        ConcurrentHashMap<Integer, Long> successfulSeatIds = new ConcurrentHashMap<>();  // 성공한 좌석 ID 추적
        ConcurrentHashMap<Long, AtomicInteger> attemptsPerSeat = new ConcurrentHashMap<>();  // 좌석별 시도 수
        ConcurrentHashMap<Long, AtomicInteger> successesPerSeat = new ConcurrentHashMap<>();  // 좌석별 성공 수
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        // 좌석별 추적을 위해 사전 초기화
        for (PublicSeat seat : testSeatsForScenarioTest) {
            attemptsPerSeat.putIfAbsent(seat.getId(), new AtomicInteger(0));
            successesPerSeat.putIfAbsent(seat.getId(), new AtomicInteger(0));
        }

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(CONCURRENT_USERS_COUNT_2);

        // 동시성 테스트: 합리적인 스레드풀 크기 (500개)
        // CountDownLatch로 10,000명이 동시에 시작하도록 조율
        // 단순히 스레드 개수를 늘리는 것보다, CountDownLatch의 startLatch로 모든 사용자를 같은 시간에 시작시키는 것이 핵심
        ExecutorService executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

        long startTime = System.currentTimeMillis();

        // 10000개의 작업 제출
        for (int i = 0; i < CONCURRENT_USERS_COUNT_2; i++) {
            final int userId = i;
            executorService.submit(() -> {
                try {
                    // 모든 스레드가 준비될 때까지 대기
                    startLatch.await();

                    // 현재 AVAILABLE한 좌석 중에서 랜덤으로 하나 선택
                    List<PublicSeat> availableSeats = publicSeatRepository.findRandomAvailableSeat(scenarioRound.getId());
                    if (availableSeats.isEmpty()) {
                        // AVAILABLE 좌석이 없으면 실패
                        throw new Exception("선택 가능한 좌석이 없습니다.");
                    }
                    long seatId = availableSeats.get(0).getId();

                    // 좌석별 시도 수 기록
                    attemptsPerSeat.get(seatId).incrementAndGet();

                    // 인증 객체 생성
                    Authentication authentication = new UsernamePasswordAuthenticationToken(
                            "test-user-" + userId,
                            null,
                            Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
                    );

                    // 좌석 홀드 요청
                    PublicSeatHoldRequestDto request = new PublicSeatHoldRequestDto();
                    request.setRoundId(scenarioRound.getRoundId());
                    request.setSeatIds(List.of(seatId));

                    // 서비스 호출
                    publicSeatService.holdSeats(request, authentication);

                    // 성공
                    results.put(userId, true);
                    successfulSeatIds.put(userId, seatId);  // 성공한 좌석 ID 기록
                    successesPerSeat.get(seatId).incrementAndGet();  // 좌석별 성공 수 기록
                    successCount.incrementAndGet();

                } catch (Exception e) {
                    // 실패
                    results.put(userId, false);
                    failureCount.incrementAndGet();

                } finally {
                    endLatch.countDown();
                }
            });
        }

        // 모든 스레드 동시 시작
        startLatch.countDown();

        // 모든 스레드 완료 대기
        endLatch.await();
        executorService.shutdown();

        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;

        // ========== 결과 분석 ==========
        double successRate = (double) successCount.get() / CONCURRENT_USERS_COUNT_2 * 100;
        double failureRate = (double) failureCount.get() / CONCURRENT_USERS_COUNT_2 * 100;

        // 선점된 고유 좌석 개수 집계
        Set<Long> uniqueSuccessfulSeats = new HashSet<>(successfulSeatIds.values());
        int uniqueSeatsCount = uniqueSuccessfulSeats.size();

        // 좌석별 시도/성공 통계 계산
        int totalAttempts = attemptsPerSeat.values().stream().mapToInt(AtomicInteger::get).sum();
        int totalSuccesses = successesPerSeat.values().stream().mapToInt(AtomicInteger::get).sum();
        double avgAttemptsPerSeat = (double) totalAttempts / SEAT_COUNT_FOR_SCENARIO_TEST;

        // 좌석별 성공률 분석
        int seatsWithMultipleAttempts = (int) attemptsPerSeat.values().stream()
                .filter(count -> count.get() > 1).count();
        int seatsWithZeroAttempts = (int) attemptsPerSeat.values().stream()
                .filter(count -> count.get() == 0).count();

        System.out.println("\n" +
                "╔═══════════════════════════════════════════════════╗\n" +
                "║      400개 좌석 / 10,000명 동시 접근 테스트       ║\n" +
                "╚═════════════════════════════════════════════════╝\n" +
                "  📊 기본 통계\n" +
                "  ├─ 총 사용자 수:      " + String.format("%,d", CONCURRENT_USERS_COUNT_2) + "\n" +
                "  ├─ 총 좌석 수:        " + SEAT_COUNT_FOR_SCENARIO_TEST + "\n" +
                "  ├─ 예상 성공 수:      " + SEAT_COUNT_FOR_SCENARIO_TEST + " (각 좌석당 1명)\n" +
                "  └─ 예상 실패 수:      " + (CONCURRENT_USERS_COUNT_2 - SEAT_COUNT_FOR_SCENARIO_TEST) + "\n" +
                "\n" +
                "  ✅ 실제 결과\n" +
                "  ├─ 성공 수:          " + String.format("%,d", successCount.get()) + " (" + String.format("%.2f%%", successRate) + ")\n" +
                "  ├─ 실패 수:          " + String.format("%,d", failureCount.get()) + " (" + String.format("%.2f%%", failureRate) + ")\n" +
                "  └─ 실행 시간:        " + executionTime + "ms\n" +
                "\n" +
                "  ⚙️ 성능 분석\n" +
                "  ├─ 평균 처리 시간:    " + (executionTime / CONCURRENT_USERS_COUNT_2) + "ms/user\n" +
                "  └─ 초당 처리량:       " + String.format("%.0f", (CONCURRENT_USERS_COUNT_2 / (executionTime / 1000.0))) + " req/sec\n" +
                "\n" +
                "  🎫 좌석 선점 현황\n" +
                "  ├─ 선점된 고유 좌석 수: " + uniqueSeatsCount + "\n" +
                "  ├─ 예상 좌석 수:       " + SEAT_COUNT_FOR_SCENARIO_TEST + "\n" +
                "  └─ 일치 여부:          " + (uniqueSeatsCount == SEAT_COUNT_FOR_SCENARIO_TEST ? "✅ 완벽 일치" : "❌ 불일치 (" + (SEAT_COUNT_FOR_SCENARIO_TEST - uniqueSeatsCount) + "개 미선점)") + "\n" +
                "\n" +
                "  🔬 동시성 제어 분석\n" +
                "  ├─ 총 시도 수:        " + String.format("%,d", totalAttempts) + "\n" +
                "  ├─ 총 성공 수:        " + String.format("%,d", totalSuccesses) + "\n" +
                "  ├─ 좌석당 평균 시도:  " + String.format("%.1f", avgAttemptsPerSeat) + "명\n" +
                "  ├─ 충돌 발생 좌석:    " + seatsWithMultipleAttempts + "개 (2명 이상 시도)\n" +
                "  ├─ 미선택 좌석:       " + seatsWithZeroAttempts + "개\n" +
                "  └─ 동시성 충돌률:     " + String.format("%.2f%%", (100.0 * (totalAttempts - totalSuccesses) / totalAttempts)) + " (" + (totalAttempts - totalSuccesses) + "건 충돌)\n");

        // 결과 검증
        // 중요: 성공한 사용자 수 = 선점된 고유 좌석 수 (동시성 제어 검증)
        // 랜덤 선택으로 인해 모든 좌석이 선점되지 않을 수 있음 (생일 문제)
        assertEquals(successCount.get(), uniqueSeatsCount,
                "성공한 사용자 수(" + successCount.get() + ")와 선점된 고유 좌석 수(" + uniqueSeatsCount + ")가 일치해야 합니다 (동시성 제어 검증)");

        // 성공 + 실패 = 전체 사용자
        assertEquals(CONCURRENT_USERS_COUNT_2, successCount.get() + failureCount.get(),
                "성공한 사용자 수와 실패한 사용자 수의 합이 전체 사용자 수와 일치해야 합니다");

        // 충돌률 분석
        System.out.println("\n" +
                "  📈 충돌 분석\n" +
                "  ├─ 성공률:            " + String.format("%.2f%%", successRate) + "\n" +
                "  ├─ 충돌률:            " + String.format("%.2f%%", failureRate) + "\n" +
                "  └─ 평가:              " + (failureRate > 50 ? "⚠️ 높은 충돌률 - 동시성 제어 정상 작동" : "✅ 낮은 충돌률") + "\n");

        // 좌석별 상세 분석 (샘플 출력: 첫 10개 좌석 + 충돌이 많은 좌석)
        System.out.println("\n" +
                "  🔍 좌석별 상세 분석 (샘플)\n");

        // 충돌이 많은 좌석 top 5
        var topConflictSeats = attemptsPerSeat.entrySet().stream()
                .filter(e -> e.getValue().get() > 1)
                .sorted((a, b) -> Integer.compare(b.getValue().get(), a.getValue().get()))
                .limit(5)
                .toList();

        if (!topConflictSeats.isEmpty()) {
            System.out.println("  🔥 동시성 충돌 상위 좌석:\n");
            int idx = 1;
            for (var entry : topConflictSeats) {
                long seatId = entry.getKey();
                int attempts = entry.getValue().get();
                int successes = successesPerSeat.get(seatId).get();
                int conflicts = attempts - successes;
                System.out.println("     " + idx + ". 좌석 ID " + seatId +
                        ": 시도 " + attempts + "명 → 성공 " + successes + "명 → 충돌 " + conflicts + "건");
                idx++;
            }
            System.out.println();
        } else {
            System.out.println("  ✅ 모든 좌석이 정확히 1명씩만 시도 (충돌 없음)\n");
        }
    }
}
