package copy_ticket.copy_ticket.service;

import copy_ticket.copy_ticket.domain.entity.Performance;
import copy_ticket.copy_ticket.domain.entity.User;
import copy_ticket.copy_ticket.dto.PerformanceSaveRequestDto;
import copy_ticket.copy_ticket.repository.PerformanceRepository;
import copy_ticket.copy_ticket.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

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

        // 2. 사용자의 현재 공연 개수 확인
        long currentCount = performanceRepository.countByCreatedByIdAndDeletedAtIsNull(user.getId());

        // 3. 5개 이상이면 가장 오래된 공연 soft delete
        if (currentCount >= MAX_PERFORMANCES_PER_USER) {
            List<Performance> oldestList = performanceRepository.findOldestByUserIdOrderByCreatedAtAsc(user.getId());
            if (!oldestList.isEmpty()) {
                Performance toDelete = oldestList.get(0);
                toDelete.setDeletedAt(Instant.now());
                performanceRepository.save(toDelete);
            }
        }

        // 4. DTO → Entity 변환
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

        // 5. DB에 저장
        return performanceRepository.save(performance);
    }
}

