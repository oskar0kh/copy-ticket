package copy_ticket.copy_ticket.repository;

import copy_ticket.copy_ticket.domain.entity.PublicRound;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Transactional
public class PublicRoundRepositoryTest {

    @Autowired
    private PublicRoundRepository publicRoundRepository;

    private Instant now;
    private Instant openAt;
    private Instant closeAt;
    private int roundIdCounter = 1;

    // Native query에서 AT TIME ZONE 비교를 사용하므로 테스트 입력 시각을 동일 기준으로 맞춘다.
    private Instant queryInstant(Instant instant) {
        return instant.minusSeconds(9 * 3600L);
    }

    @BeforeEach
    void setUp() {
        now = Instant.now();
        openAt = now.minusSeconds(60);      // 1분 전에 오픈
        closeAt = now.plusSeconds(540);      // 9분 후에 종료 (총 10분)

        System.out.println("\n" + "=".repeat(80));
        System.out.println("📋 PublicRoundRepository Test Setup");
        System.out.println("=".repeat(80));
        System.out.println("🕐 Now:     " + now);
        System.out.println("🕐 OpenAt:  " + openAt);
        System.out.println("🕐 CloseAt: " + closeAt);
        System.out.println("=".repeat(80));
    }

    @Test
    void testFindOneBookableOpenRound_ShouldFound() {
        // Given: 현재 시간 범위 내의 OPEN 라운드 생성
        PublicRound round = PublicRound.builder()
                .roundId(roundIdCounter++)
                .status(PublicRound.RoundStatus.OPEN)
                .openAt(openAt)
                .closeAt(closeAt)
                .createdAt(now)
                .updatedAt(now)
                .build();

        publicRoundRepository.save(round);

        System.out.println("\n✅ Test 1: Find Bookable Round (Should Found)");
        System.out.println("📝 Created Round:");
        System.out.println("   - ID: " + round.getId());
        System.out.println("   - Status: " + round.getStatus());
        System.out.println("   - OpenAt: " + round.getOpenAt());
        System.out.println("   - CloseAt: " + round.getCloseAt());

        // When: 현재 시각으로 조회
        Optional<PublicRound> result = publicRoundRepository.findOneBookableOpenRound(queryInstant(now));

        // Then: 라운드를 찾아야 함
        System.out.println("\n🔍 Query Result:");
        System.out.println("   - Found: " + result.isPresent());
        if (result.isPresent()) {
            System.out.println("   - ID: " + result.get().getId());
            System.out.println("   - RoundNumber: " + result.get().getRoundId());
        }
        assertTrue(result.isPresent(), "현재 시간 범위의 OPEN 라운드를 찾아야 합니다");
        assertEquals(1, result.get().getRoundId());
    }

    @Test
    void testFindOneBookableOpenRound_OpenAtBoundary() {
        // Given: openAt 정확히 시점의 라운드
        PublicRound round = PublicRound.builder()
                .roundId(roundIdCounter++)
                .status(PublicRound.RoundStatus.OPEN)
                .openAt(openAt)
                .closeAt(closeAt)
                .createdAt(openAt)
                .updatedAt(openAt)
                .build();

        publicRoundRepository.save(round);

        System.out.println("\n✅ Test 2: Query at OpenAt Boundary");
        System.out.println("📝 Created Round:");
        System.out.println("   - OpenAt: " + round.getOpenAt());
        System.out.println("   - CloseAt: " + round.getCloseAt());
        System.out.println("   - Query Time: " + openAt + " (exactly openAt)");

        // When: openAt 정확히 시점으로 조회
        Optional<PublicRound> result = publicRoundRepository.findOneBookableOpenRound(queryInstant(openAt));

        // Then: 라운드를 찾아야 함 (openAt <= now 조건이므로)
        System.out.println("\n🔍 Query Result: " + (result.isPresent() ? "✅ Found" : "❌ Not Found"));
        assertTrue(result.isPresent(), "openAt 정확히 시점도 포함되어야 합니다 (openAt <= now)");
    }

    @Test
    void testFindOneBookableOpenRound_CloseAtBoundary() {
        // Given: closeAt 정확히 시점의 라운드
        PublicRound round = PublicRound.builder()
                .roundId(roundIdCounter++)
                .status(PublicRound.RoundStatus.OPEN)
                .openAt(openAt)
                .closeAt(closeAt)
                .createdAt(now)
                .updatedAt(now)
                .build();

        publicRoundRepository.save(round);

        System.out.println("\n✅ Test 3: Query at CloseAt Boundary");
        System.out.println("📝 Created Round:");
        System.out.println("   - OpenAt: " + round.getOpenAt());
        System.out.println("   - CloseAt: " + round.getCloseAt());
        System.out.println("   - Query Time: " + closeAt + " (exactly closeAt)");

        // When: closeAt 정확히 시점으로 조회
        Optional<PublicRound> result = publicRoundRepository.findOneBookableOpenRound(queryInstant(closeAt));

        // Then: 라운드를 찾으면 안 됨 (close_at > :now 조건이므로 등호 제외)
        System.out.println("\n🔍 Query Result: " + (result.isPresent() ? "❌ Found" : "✅ Not Found"));
        assertFalse(result.isPresent(), "closeAt 정확히 시점은 제외되어야 합니다 (close_at > now, 등호 제외)");
    }

    @Test
    void testFindOneBookableOpenRound_BeforeOpenAt() {
        // Given: 아직 오픈 시간이 아닌 라운드
        Instant futureOpenAt = now.plusSeconds(600);
        Instant futureCloseAt = futureOpenAt.plusSeconds(600);

        PublicRound round = PublicRound.builder()
                .roundId(roundIdCounter++)
                .status(PublicRound.RoundStatus.OPEN)
                .openAt(futureOpenAt)
                .closeAt(futureCloseAt)
                .createdAt(now)
                .updatedAt(now)
                .build();

        publicRoundRepository.save(round);

        System.out.println("\n✅ Test 4: Query Before OpenAt");
        System.out.println("📝 Created Round:");
        System.out.println("   - OpenAt: " + round.getOpenAt());
        System.out.println("   - CloseAt: " + round.getCloseAt());
        System.out.println("   - Query Time: " + now + " (before openAt)");

        // When: openAt 이전 시점으로 조회
        Optional<PublicRound> result = publicRoundRepository.findOneBookableOpenRound(queryInstant(now));

        // Then: 라운드를 찾으면 안 됨
        System.out.println("\n🔍 Query Result: " + (result.isPresent() ? "❌ Found" : "✅ Not Found"));
        assertFalse(result.isPresent(), "OpenAt 이전은 조회되면 안 됩니다");
    }

    @Test
    void testFindOneBookableOpenRound_AfterCloseAt() {
        // Given: 이미 종료된 라운드
        Instant pastOpenAt = now.minusSeconds(600);
        Instant pastCloseAt = now.minusSeconds(60);

        PublicRound round = PublicRound.builder()
                .roundId(roundIdCounter++)
                .status(PublicRound.RoundStatus.OPEN)
                .openAt(pastOpenAt)
                .closeAt(pastCloseAt)
                .createdAt(pastOpenAt)
                .updatedAt(pastOpenAt)
                .build();

        publicRoundRepository.save(round);

        System.out.println("\n✅ Test 5: Query After CloseAt");
        System.out.println("📝 Created Round:");
        System.out.println("   - OpenAt: " + round.getOpenAt());
        System.out.println("   - CloseAt: " + round.getCloseAt());
        System.out.println("   - Query Time: " + now + " (after closeAt)");

        // When: closeAt 이후 시점으로 조회
        Optional<PublicRound> result = publicRoundRepository.findOneBookableOpenRound(queryInstant(now));

        // Then: 라운드를 찾으면 안 됨
        System.out.println("\n🔍 Query Result: " + (result.isPresent() ? "❌ Found" : "✅ Not Found"));
        assertFalse(result.isPresent(), "CloseAt 이후는 조회되면 안 됩니다");
    }

    @Test
    void testFindOneBookableOpenRound_ClosedStatus() {
        // Given: CLOSED 상태의 라운드
        PublicRound round = PublicRound.builder()
                .roundId(roundIdCounter++)
                .status(PublicRound.RoundStatus.CLOSED)
                .openAt(openAt)
                .closeAt(closeAt)
                .createdAt(now)
                .updatedAt(now)
                .build();

        publicRoundRepository.save(round);

        System.out.println("\n✅ Test 6: Query with CLOSED Status");
        System.out.println("📝 Created Round:");
        System.out.println("   - Status: " + round.getStatus());
        System.out.println("   - OpenAt: " + round.getOpenAt());
        System.out.println("   - CloseAt: " + round.getCloseAt());

        // When: 조회
        Optional<PublicRound> result = publicRoundRepository.findOneBookableOpenRound(queryInstant(now));

        // Then: 라운드를 찾으면 안 됨 (OPEN 상태만 조회)
        System.out.println("\n🔍 Query Result: " + (result.isPresent() ? "❌ Found" : "✅ Not Found"));
        assertFalse(result.isPresent(), "CLOSED 상태는 조회되면 안 됩니다");
    }

    @Test
    void testFindOneBookableOpenRound_MultipleRounds() {
        // Given: 여러 OPEN 라운드
        PublicRound round1 = PublicRound.builder()
                .roundId(roundIdCounter++)
                .status(PublicRound.RoundStatus.OPEN)
                .openAt(openAt)
                .closeAt(closeAt)
                .createdAt(now)
                .updatedAt(now)
                .build();

        Instant openAt2 = closeAt.plusSeconds(1200);
        Instant closeAt2 = openAt2.plusSeconds(600);

        PublicRound round2 = PublicRound.builder()
                .roundId(2)
                .status(PublicRound.RoundStatus.OPEN)
                .openAt(openAt2)
                .closeAt(closeAt2)
                .createdAt(now)
                .updatedAt(now)
                .build();

        publicRoundRepository.save(round1);
        publicRoundRepository.save(round2);

        System.out.println("\n✅ Test 7: Multiple Rounds");
        System.out.println("📝 Round 1:");
        System.out.println("   - RoundNumber: " + round1.getRoundId());
        System.out.println("   - OpenAt: " + round1.getOpenAt());
        System.out.println("   - CloseAt: " + round1.getCloseAt());
        System.out.println("📝 Round 2:");
        System.out.println("   - RoundNumber: " + round2.getRoundId());
        System.out.println("   - OpenAt: " + openAt2);
        System.out.println("   - CloseAt: " + closeAt2);
        System.out.println("🔍 Query Time: " + now);

        // When: 현재 시각으로 조회
        Optional<PublicRound> result = publicRoundRepository.findOneBookableOpenRound(queryInstant(now));

        // Then: Round 1을 찾아야 함
        System.out.println("\n🔍 Query Result: " + (result.isPresent() ? "✅ Found Round " + result.get().getRoundId() : "❌ Not Found"));
        assertTrue(result.isPresent(), "현재 시간 범위의 라운드를 찾아야 합니다");
        assertEquals(1, result.get().getRoundId(), "가장 빠른 openAt을 가진 Round 1을 반환해야 합니다");
    }
}
