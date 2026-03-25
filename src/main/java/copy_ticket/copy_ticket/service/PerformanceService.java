package copy_ticket.copy_ticket.service;

import copy_ticket.copy_ticket.domain.entity.Performance;
import copy_ticket.copy_ticket.domain.entity.User;
import copy_ticket.copy_ticket.dto.PerformanceResponseDto;
import copy_ticket.copy_ticket.dto.PerformanceSaveRequestDto;
import copy_ticket.copy_ticket.repository.PerformanceRepository;
import copy_ticket.copy_ticket.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PerformanceService {

    private final PerformanceRepository performanceRepository;
    private final UserRepository userRepository;

    private static final long MAX_PERFORMANCES_PER_USER = 5L;

    @Transactional
    public Performance savePerformance(PerformanceSaveRequestDto saveRequestDto, String userId) {
        // 1. 사용자 조회
        User user = userRepository.findByUserIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        // 2. 같은 사용자의 같은 goods_code가 이미 있으면 soft delete 처리
        //    goods_code unique 제약 충돌을 피하기 위해 기존 값은 null로 비움
        performanceRepository.findByCreatedByIdAndGoodsCodeAndDeletedAtIsNull(user.getId(), saveRequestDto.getGoodsCode())
                .ifPresent(existing -> {
                Instant now = Instant.now();
                existing.setDeletedAt(now);
                existing.setUpdatedAt(now);
                existing.setGoodsCode(null);
                performanceRepository.saveAndFlush(existing);
                });

        // 3. 사용자의 현재 공연 개수 확인 (삭제되지 않은 것만)
        long currentCount = performanceRepository.countByCreatedByIdAndDeletedAtIsNull(user.getId());

        // 4. 5개 이상이면 가장 오래된 공연 soft delete
        if (currentCount >= MAX_PERFORMANCES_PER_USER) {
            List<Performance> oldestList = performanceRepository.findOldestByUserIdOrderByCreatedAtAsc(user.getId());
            if (!oldestList.isEmpty()) {
                Performance toDelete = oldestList.get(0);
                toDelete.setDeletedAt(Instant.now());
                performanceRepository.save(toDelete);
            }
        }

        // 5. DTO → Entity 변환
        Performance performance = new Performance();
        performance.setSourceUrl(saveRequestDto.getSourceUrl());
        performance.setTitle(saveRequestDto.getTitle());
        performance.setImageUrl(saveRequestDto.getImageUrl());
        performance.setStartDate(saveRequestDto.getStartDate());
        performance.setEndDate(saveRequestDto.getEndDate());
        performance.setLink(saveRequestDto.getLink());
        performance.setGoodsCode(saveRequestDto.getGoodsCode());
        performance.setGoodsName(saveRequestDto.getGoodsName());
        performance.setPlaceCode(saveRequestDto.getPlaceCode());
        performance.setPlaceName(saveRequestDto.getPlaceName());
        performance.setPlayDate(saveRequestDto.getPlayDate());
        performance.setPlayStartDate(saveRequestDto.getPlayStartDate());
        performance.setPlayEndDate(saveRequestDto.getPlayEndDate());
        performance.setCreatedBy(user);
        performance.setCreatedAt(Instant.now());
        performance.setUpdatedAt(Instant.now());
        performance.setDeletedAt(null);

        // 6. DB에 저장
        return performanceRepository.save(performance);
    }

    /**
     * 사용자의 저장된 공연 목록을 조회 (title만 반환)
     *
     * @param userId 사용자 ID
     * @return 저장된 공연의 title 목록
     */
    @Transactional(readOnly = true)
    public List<String> getPerformanceTitleListByUserId(String userId) {
        User user = userRepository.findByUserIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        return performanceRepository.findByCreatedByIdAndDeletedAtIsNullOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(Performance::getTitle)
                .collect(Collectors.toList());
    }

    /**
     * 사용자의 저장된 공연 목록을 조회 (ID, title, goodsCode)
     *
     * @param userId 사용자 ID
     * @return 저장된 공연의 목록 (ID, title, goodsCode 포함)
     */
    @Transactional(readOnly = true)
    public List<PerformanceListItemDto> getPerformanceListByUserId(String userId) {
        User user = userRepository.findByUserIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        return performanceRepository.findByCreatedByIdAndDeletedAtIsNullOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(perf -> new PerformanceListItemDto(perf.getId(), perf.getTitle(), perf.getGoodsCode()))
                .collect(Collectors.toList());
    }

    /**
     * 특정 공연 정보를 조회
     * 보안: 조회하려는 사용자가 해당 공연의 소유자인지 확인
     *
     * @param performanceId 공연 ID
     * @param userId 사용자 ID
     * @return 공연 상세 정보 (PerformanceResponseDto)
     */
    @Transactional(readOnly = true)
    public PerformanceResponseDto getPerformanceById(Long performanceId, String userId) {
        User user = userRepository.findByUserIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        Performance performance = performanceRepository.findById(performanceId)
                .orElseThrow(() -> new RuntimeException("Performance not found: " + performanceId));

        // 보안 확인: 조회하려는 공연이 해당 사용자의 것인지 확인
        if (!performance.getCreatedBy().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized access to performance");
        }

        // 삭제된 공연은 조회 불가
        if (performance.getDeletedAt() != null) {
            throw new RuntimeException("Performance is deleted");
        }

        return PerformanceResponseDto.fromEntity(performance);
    }

    /**
     * DTO: 공연 목록 아이템 (ID + title + goodsCode)
     */
    public static class PerformanceListItemDto {
        public Long id;
        public String title;
        public String goodsCode;

        public PerformanceListItemDto(Long id, String title, String goodsCode) {
            this.id = id;
            this.title = title;
            this.goodsCode = goodsCode;
        }

        public Long getId() {
            return id;
        }

        public String getTitle() {
            return title;
        }

        public String getGoodsCode() {
            return goodsCode;
        }
    }
}

