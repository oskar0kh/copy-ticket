package copy_ticket.copy_ticket.repository;

import copy_ticket.copy_ticket.domain.entity.Performance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PerformanceRepository extends JpaRepository<Performance, Long> {

    // 사용자의 soft delete되지 않은 공연 개수 조회
    long countByCreatedByIdAndDeletedAtIsNull(Long userId);

    // 사용자의 가장 오래된 공연 조회 (soft delete되지 않은 것 중에서)
    @Query("SELECT p FROM Performance p WHERE p.createdBy.id = :userId AND p.deletedAt IS NULL ORDER BY p.createdAt ASC")
    List<Performance> findOldestByUserIdOrderByCreatedAtAsc(@Param("userId") Long userId);

    // 사용자의 모든 공연 조회 (soft delete되지 않은 것만)
    List<Performance> findByCreatedByIdAndDeletedAtIsNullOrderByCreatedAtDesc(Long userId);
}

