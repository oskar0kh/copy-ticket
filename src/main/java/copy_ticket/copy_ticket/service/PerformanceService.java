package copy_ticket.copy_ticket.service;

import copy_ticket.copy_ticket.domain.entity.Performance;
import copy_ticket.copy_ticket.domain.entity.User;
import copy_ticket.copy_ticket.dto.PerformanceResponseDto;
import copy_ticket.copy_ticket.dto.PerformanceSaveRequestDto;
import copy_ticket.copy_ticket.repository.PerformanceRepository;
import copy_ticket.copy_ticket.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PerformanceService {

    private final PerformanceRepository performanceRepository;
    private final UserRepository userRepository;

    private static final long MAX_PERFORMANCES_PER_USER = 5L;

    @Transactional
    public Performance savePerformance(PerformanceSaveRequestDto saveRequestDto, String userId) {
        // 1. 사용자 조회
        User user = userRepository.findUserByUserIdWithoutSoftDeleted(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        // 2. goodsCode 기반 기존 레코드 확인 (soft delete되지 않은 것만)
        if (saveRequestDto.getGoodsCode() != null) {
            List<Performance> existingList = performanceRepository.findSameGoodsCodePerformance(
                    user.getId(), saveRequestDto.getGoodsCode());

            if (!existingList.isEmpty()) {
                // 기존 레코드 있으면 soft delete
                Performance existing = existingList.get(0);
                existing.setDeletedAt(Instant.now());
                performanceRepository.save(existing);
                log.info("Soft deleted existing performance with goodsCode: {}", saveRequestDto.getGoodsCode());
            }
        }

        // 3. 사용자의 현재 공연 개수 확인 (삭제되지 않은 것만)
        long currentCount = performanceRepository.countNotSoftDeletedPerformance(user.getId());

        // 4. 5개 이상이면 가장 오래된 공연 soft delete
        if (currentCount >= MAX_PERFORMANCES_PER_USER) {
            List<Performance> oldestList = performanceRepository.findOldestPerformance(user.getId());
            if (!oldestList.isEmpty()) {
                Performance toDelete = oldestList.get(0);
                toDelete.setDeletedAt(Instant.now());
                performanceRepository.save(toDelete);
            }
        }

        // 5. DTO → Entity 변환 (13개 핵심 필드)
        Performance performance = new Performance();
        performance.setSourceUrl(saveRequestDto.getSourceUrl());
        performance.setGoodsCode(saveRequestDto.getGoodsCode());
        performance.setGoodsName(saveRequestDto.getGoodsName());
        performance.setSubGoodsName(saveRequestDto.getSubGoodsName());
        performance.setPlaceName(saveRequestDto.getPlaceName());
        performance.setViewRateName(saveRequestDto.getViewRateName());
        performance.setRunningTime(saveRequestDto.getRunningTime());
        performance.setPlayStartDate(saveRequestDto.getPlayStartDate());
        performance.setPlayEndDate(saveRequestDto.getPlayEndDate());
        performance.setGoodsLargeImageUrl(saveRequestDto.getGoodsLargeImageUrl());
        performance.setTicketOpenDate(saveRequestDto.getTicketOpenDate());
        performance.setBookingEndDate(saveRequestDto.getBookingEndDate());
        performance.setTicketCastCount(saveRequestDto.getTicketCastCount());
        performance.setWeekRank(saveRequestDto.getWeekRank());
        performance.setCreatedBy(user);
        performance.setCreatedAt(Instant.now());
        performance.setUpdatedAt(Instant.now());
        performance.setDeletedAt(null);

        // 6. DB에 저장
        return performanceRepository.save(performance);
    }

    /**
     * 사용자의 저장된 공연 목록을 조회 (goodsName 반환)
     *
     * @param userId 사용자 ID
     * @return 저장된 공연의 goodsName 목록
     */
    @Transactional(readOnly = true)
    public List<String> getPerformanceTitleListByUserId(String userId) {
        User user = userRepository.findUserByUserIdWithoutSoftDeleted(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        return performanceRepository.findAllPerformanceByUser(user.getId())
                .stream()
                .map(Performance::getGoodsName)
                .collect(Collectors.toList());
    }

    /**
     * 사용자의 저장된 공연 목록을 조회 (ID, goodsName 포함)
     *
     * @param userId 사용자 ID
     * @return 저장된 공연의 목록 (ID, goodsName 포함)
     */
    @Transactional(readOnly = true)
    public List<PerformanceListItemDto> getPerformanceListByUserId(String userId) {
        User user = userRepository.findUserByUserIdWithoutSoftDeleted(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        return performanceRepository.findAllPerformanceByUser(user.getId())
                .stream()
                .map(perf -> new PerformanceListItemDto(perf.getId(), perf.getGoodsName()))
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
        User user = userRepository.findUserByUserIdWithoutSoftDeleted(userId)
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
     * 공연을 soft delete로 삭제
     * 보안: 삭제하려는 사용자가 해당 공연의 소유자인지 확인
     *
     * @param performanceId 삭제할 공연 ID
     * @param userId 삭제를 요청한 사용자 ID
     * @throws RuntimeException 공연을 찾을 수 없거나 권한이 없는 경우
     */
    @Transactional
    public void deletePerformance(Long performanceId, String userId) {
        User user = userRepository.findUserByUserIdWithoutSoftDeleted(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        Performance performance = performanceRepository.findById(performanceId)
                .orElseThrow(() -> new RuntimeException("Performance not found: " + performanceId));

        // 보안 확인: 삭제하려는 공연이 해당 사용자의 것인지 확인
        if (!performance.getCreatedBy().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized access to performance");
        }

        // 이미 삭제된 공연은 삭제할 수 없음
        if (performance.getDeletedAt() != null) {
            throw new RuntimeException("Performance is already deleted");
        }

        // Soft delete: deleted_at 설정, updated_at 업데이트
        Instant now = Instant.now();
        performance.setDeletedAt(now);
        performance.setUpdatedAt(now);
        performanceRepository.save(performance);
    }

    /**
     * DTO: 공연 목록 아이템 (ID + goodsName)
     */
    public static class PerformanceListItemDto {
        public Long id;
        public String goodsName;

        public PerformanceListItemDto(Long id, String goodsName) {
            this.id = id;
            this.goodsName = goodsName;
        }

        public Long getId() {
            return id;
        }

        public String getGoodsName() {
            return goodsName;
        }
    }
}
