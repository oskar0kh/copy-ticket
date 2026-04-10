// 'users' 테이블 (User 엔티티) CRUD용 인터페이스

package copy_ticket.copy_ticket.repository;

import copy_ticket.copy_ticket.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // 로그인 시, 사용자 아이디로 User 엔티티 조회하는 메서드
    @Query(value = """
        SELECT *
        FROM users
        WHERE user_id = :userId
        LIMIT 1
        """, nativeQuery = true)
    Optional<User> findUserByUserId(@Param("userId") String userId);

    // 로그인 시, 사용자 아이디로 User 엔티티 조회하는 메서드 (soft delete된 레코드는 제외)
    @Query(value = """
        SELECT *
        FROM users
        WHERE user_id = :userId
          AND deleted_at IS NULL
        LIMIT 1
        """, nativeQuery = true)
    Optional<User> findUserByUserIdWithoutSoftDeleted(@Param("userId") String userId);

    // 회원가입 시, 사용자 아이디 중복 체크하는 메서드
    @Query(value = """
        SELECT EXISTS (
            SELECT 1
            FROM users
            WHERE user_id = :userId
        )
        """, nativeQuery = true)
    boolean existsUsersByUserId(@Param("userId") String userId);

    // 회원가입 시, 사용자 아이디 중복 체크하는 메서드 (soft delete된 레코드는 제외)
    @Query(value = """
        SELECT EXISTS (
            SELECT 1
            FROM users
            WHERE user_id = :userId
              AND deleted_at IS NULL
        )
        """, nativeQuery = true)
    boolean existsUserByUserIdWithoutSoftDeleted(@Param("userId") String userId);
}
