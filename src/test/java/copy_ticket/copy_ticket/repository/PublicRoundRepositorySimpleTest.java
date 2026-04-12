package copy_ticket.copy_ticket.repository;

import copy_ticket.copy_ticket.domain.entity.PublicRound;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PublicRoundRepository.findOneBookableOpenRound() 테스트
 * 
 * 현재 시간을 기준으로 openAt <= now < closeAt 범위의 OPEN 라운드를 찾을 수 있는지 검증
 */
@SpringBootTest
@TestPropertySource(properties = {
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
public class PublicRoundRepositorySimpleTest {

    @Autowired
    private PublicRoundRepository publicRoundRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Instant now;
    private Instant openAt;
    private Instant closeAt;

    // Native query에서 AT TIME ZONE 비교를 사용하므로 테스트 입력 시각을 동일 기준으로 맞춘다.
    private Instant queryInstant(Instant instant) {
        return instant.minusSeconds(9 * 3600L);
    }

    @BeforeEach
    void setUp() {
        // 테스트 전 公開라운드 테이블 정리
        jdbcTemplate.update("DELETE FROM public_rounds");

        now = Instant.now();
        openAt = now.minusSeconds(60);      // 1분 전에 오픈
        closeAt = now.plusSeconds(540);     // 9분 후에 종료 (총 10분)

        System.out.println("\n" + "=".repeat(100));
        System.out.println("📋 PublicRoundRepository Test - findOneBookableOpenRound()");
        System.out.println("=".repeat(100));
        System.out.println("🕐 Now:     " + now);
        System.out.println("🕐 OpenAt:  " + openAt + " (1분 전)");
        System.out.println("🕐 CloseAt: " + closeAt + " (9분 후)");
        System.out.println("=".repeat(100));
    }

    @Test
    void testShouldFoundWhenInRange() {
        // Given: 현재 시간 범위 내의 OPEN 라운드
        PublicRound round = PublicRound.builder()
                .roundId((int)(System.currentTimeMillis() % 100000))
                .status(PublicRound.RoundStatus.OPEN)
                .openAt(openAt)
                .closeAt(closeAt)
                .createdAt(now)
                .updatedAt(now)
                .build();

        publicRoundRepository.save(round);

        System.out.println("\n✅ Test 1: Query Range내의 라운드 조회");
        System.out.println("📝 생성된 라운드:");
        System.out.println("   - RoundNumber: " + round.getRoundId());
        System.out.println("   - Status: " + round.getStatus());
        System.out.println("   - OpenAt: " + round.getOpenAt());
        System.out.println("   - CloseAt: " + round.getCloseAt());
        System.out.println("🔍 Query Time: " + now + " (Range 범위 내)");

        // When
        Optional<PublicRound> result = publicRoundRepository.findOneBookableOpenRound(queryInstant(now));

        // Then
        System.out.println("📊 결과: " + (result.isPresent() ? "✅ Found" : "❌ Not Found"));
        assertTrue(result.isPresent(), "현재 시간이 Range 내이므로 라운드를 찾아야 합니다");
        assertEquals(round.getRoundId(), result.get().getRoundId());
    }

    @Test
    void testShouldFoundAtOpenAtBoundary() {
        // Given
        PublicRound round = PublicRound.builder()
                .roundId((int)(System.currentTimeMillis() % 100000))
                .status(PublicRound.RoundStatus.OPEN)
                .openAt(openAt)
                .closeAt(closeAt)
                .createdAt(openAt)
                .updatedAt(openAt)
                .build();

        publicRoundRepository.save(round);

        System.out.println("\n✅ Test 2: OpenAt 경계값 테스트");
        System.out.println("📝 라운드:");
        System.out.println("   - OpenAt: " + openAt);
        System.out.println("   - CloseAt: " + closeAt);
        System.out.println("🔍 Query Time: " + openAt + " (정확히 openAt)");

        // When: openAt 정확히 시점으로 조회
        Optional<PublicRound> result = publicRoundRepository.findOneBookableOpenRound(queryInstant(openAt));

        // Then: openAt <= now 조건이므로 포함되어야 함
        System.out.println("📊 결과: " + (result.isPresent() ? "✅ Found" : "❌ Not Found"));
        assertTrue(result.isPresent(), "openAt 정확히 시점도 포함되어야 합니다 (openAt <= now)");
    }

    @Test
    void testShouldNotFoundAtCloseAtBoundary() {
        // Given
        PublicRound round = PublicRound.builder()
                .roundId((int)(System.currentTimeMillis() % 100000))
                .status(PublicRound.RoundStatus.OPEN)
                .openAt(openAt)
                .closeAt(closeAt)
                .createdAt(now)
                .updatedAt(now)
                .build();

        publicRoundRepository.save(round);

        System.out.println("\n✅ Test 3: CloseAt 경계값 테스트");
        System.out.println("📝 라운드:");
        System.out.println("   - OpenAt: " + openAt);
        System.out.println("   - CloseAt: " + closeAt);
        System.out.println("🔍 Query Time: " + closeAt + " (정확히 closeAt)");

        // When: closeAt 정확히 시점으로 조회
        Optional<PublicRound> result = publicRoundRepository.findOneBookableOpenRound(queryInstant(closeAt));

        // Then: close_at > :now 조건이므로 등호 제외
        System.out.println("📊 결과: " + (result.isPresent() ? "❌ Found (오류!)" : "✅ Not Found"));
        assertFalse(result.isPresent(), "closeAt 정확히 시점은 제외되어야 합니다 (close_at > now, 등호 제외)");
    }

    @Test
    void testShouldNotFoundBeforeOpenAt() {
        // Given: 아직 오픈 시간이 아닌 라운드
        Instant futureOpenAt = now.plusSeconds(600);
        Instant futureCloseAt = futureOpenAt.plusSeconds(600);

        PublicRound round = PublicRound.builder()
                .roundId((int)(System.currentTimeMillis() % 100000))
                .status(PublicRound.RoundStatus.OPEN)
                .openAt(futureOpenAt)
                .closeAt(futureCloseAt)
                .createdAt(now)
                .updatedAt(now)
                .build();

        publicRoundRepository.save(round);

        System.out.println("\n✅ Test 4: OpenAt 이전 시간 조회");
        System.out.println("📝 라운드:");
        System.out.println("   - OpenAt: " + futureOpenAt + " (10분 후)");
        System.out.println("   - CloseAt: " + futureCloseAt);
        System.out.println("🔍 Query Time: " + now + " (openAt 이전)");

        // When
        Optional<PublicRound> result = publicRoundRepository.findOneBookableOpenRound(queryInstant(now));

        // Then
        System.out.println("📊 결과: " + (result.isPresent() ? "❌ Found (오류!)" : "✅ Not Found"));
        assertFalse(result.isPresent(), "OpenAt 이전은 조회되면 안 됩니다");
    }

    @Test
    void testShouldNotFoundAfterCloseAt() {
        // Given: 이미 종료된 라운드
        Instant pastOpenAt = now.minusSeconds(600);
        Instant pastCloseAt = now.minusSeconds(60);

        PublicRound round = PublicRound.builder()
                .roundId((int)(System.currentTimeMillis() % 100000))
                .status(PublicRound.RoundStatus.OPEN)
                .openAt(pastOpenAt)
                .closeAt(pastCloseAt)
                .createdAt(pastOpenAt)
                .updatedAt(pastOpenAt)
                .build();

        publicRoundRepository.save(round);

        System.out.println("\n✅ Test 5: CloseAt 이후 시간 조회");
        System.out.println("📝 라운드:");
        System.out.println("   - OpenAt: " + pastOpenAt);
        System.out.println("   - CloseAt: " + pastCloseAt + " (1분 전 종료)");
        System.out.println("🔍 Query Time: " + now + " (closeAt 이후)");

        // When
        Optional<PublicRound> result = publicRoundRepository.findOneBookableOpenRound(queryInstant(now));

        // Then
        System.out.println("📊 결과: " + (result.isPresent() ? "❌ Found (오류!)" : "✅ Not Found"));
        assertFalse(result.isPresent(), "CloseAt 이후는 조회되면 안 됩니다");
    }

    @Test
    void testShouldNotFoundClosedStatus() {
        // Given: CLOSED 상태의 라운드
        PublicRound round = PublicRound.builder()
                .roundId((int)(System.currentTimeMillis() % 100000))
                .status(PublicRound.RoundStatus.CLOSED)
                .openAt(openAt)
                .closeAt(closeAt)
                .createdAt(now)
                .updatedAt(now)
                .build();

        publicRoundRepository.save(round);

        System.out.println("\n✅ Test 6: CLOSED 상태 라운드 조회");
        System.out.println("📝 라운드:");
        System.out.println("   - Status: " + round.getStatus());
        System.out.println("   - OpenAt: " + round.getOpenAt());
        System.out.println("   - CloseAt: " + round.getCloseAt());

        // When
        Optional<PublicRound> result = publicRoundRepository.findOneBookableOpenRound(queryInstant(now));

        // Then
        System.out.println("📊 결과: " + (result.isPresent() ? "❌ Found (오류!)" : "✅ Not Found"));
        assertFalse(result.isPresent(), "CLOSED 상태는 조회되면 안 됩니다");
    }

    @Test
    void testReturnEarliestWhenMultipleRounds() {
        // Given: 여러 OPEN 라운드 생성 (현재 시간 범위)
        
        PublicRound round1 = PublicRound.builder()
                .roundId(100001)
                .status(PublicRound.RoundStatus.OPEN)
                .openAt(openAt)
                .closeAt(closeAt)
                .createdAt(now)
                .updatedAt(now)
                .build();

        Instant openAt2 = closeAt.plusSeconds(1200);
        Instant closeAt2 = openAt2.plusSeconds(600);

        PublicRound round2 = PublicRound.builder()
                .roundId(100002)
                .status(PublicRound.RoundStatus.OPEN)
                .openAt(openAt2)
                .closeAt(closeAt2)
                .createdAt(now)
                .updatedAt(now)
                .build();

        publicRoundRepository.save(round1);
        publicRoundRepository.save(round2);

        System.out.println("\n✅ Test 7: 여러 라운드 중 조회");
        System.out.println("📝 Round 1:");
        System.out.println("   - RoundNumber: 100001");
        System.out.println("   - OpenAt: " + openAt);
        System.out.println("   - CloseAt: " + closeAt + " (현재 범위 내)");
        System.out.println("📝 Round 2:");
        System.out.println("   - RoundNumber: 100002");
        System.out.println("   - OpenAt: " + openAt2 + " (미래)");
        System.out.println("   - CloseAt: " + closeAt2);
        System.out.println("🔍 Query Time: " + now);

        // When
        Optional<PublicRound> result = publicRoundRepository.findOneBookableOpenRound(queryInstant(now));

        // Then: Round 1을 반환해야 함
        System.out.println("📊 결과: " + (result.isPresent() ? result.get().getRoundId() + " (✅ Correct)" : "Not Found"));
        assertTrue(result.isPresent(), "현재 범위 내의 라운드를 찾아야 합니다");
        assertEquals(100001, result.get().getRoundId(), "가장 빠른 openAt을 가진 Round 1을 반환해야 합니다");
    }
}
