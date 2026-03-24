// 'users' 테이블 (User 엔티티) CRUD용 인터페이스

package copy_ticket.copy_ticket.repository;

import copy_ticket.copy_ticket.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // 로그인 시, 사용자 아이디로 User 엔티티 조회하는 메서드
    Optional<User> findByUserId(String userId);

    Optional<User> findByUserIdAndDeletedAtIsNull(String userId);

    // 회원가입 시, 사용자 아이디 중복 체크하는 메서드
    boolean existsByUserId(String userId);

    boolean existsByUserIdAndDeletedAtIsNull(String userId);
}
