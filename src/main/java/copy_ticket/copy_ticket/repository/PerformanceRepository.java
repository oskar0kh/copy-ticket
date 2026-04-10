package copy_ticket.copy_ticket.repository;

import copy_ticket.copy_ticket.domain.entity.Performance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PerformanceRepository extends JpaRepository<Performance, Long> {

    // 사용자의 soft delete되지 않은 공연 개수 조회
    @Query("""
            SELECT COUNT(p)
            FROM Performance p
            WHERE p.createdBy.id = :userId
                AND p.deletedAt IS NULL
            """)
    long countNotSoftDeletedPerformance(@Param("userId") Long userId);

    // 사용자의 가장 오래된 공연 조회 (soft delete되지 않은 것 중에서)
    @Query("""
            SELECT p
            FROM Performance p
            WHERE p.createdBy.id = :userId
                AND p.deletedAt IS NULL
            ORDER BY p.createdAt ASC
            """)
    List<Performance> findOldestPerformance(@Param("userId") Long userId);

    // 사용자의 모든 공연 조회 (soft delete되지 않은 것만)
    @Query("""
            SELECT p
            FROM Performance p
            WHERE p.createdBy.id = :userId
                AND p.deletedAt IS NULL
            ORDER BY p.createdAt DESC
            """)
    List<Performance> findAllPerformanceByUser(@Param("userId") Long userId);

    // 같은 goodsCode 기존 레코드 조회 (soft delete되지 않은 것만)
    @Query("""
            SELECT p
            FROM Performance p
            WHERE p.createdBy.id = :userId
                AND p.goodsCode = :goodsCode
                AND p.deletedAt IS NULL
            """)
    List<Performance> findSameGoodsCodePerformance(@Param("userId") Long userId, @Param("goodsCode") String goodsCode);
}

