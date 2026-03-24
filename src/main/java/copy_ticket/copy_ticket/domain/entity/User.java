package copy_ticket.copy_ticket.domain.entity;

import copy_ticket.copy_ticket.config.time.KstDateTimeUtils;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * 회원 — 로그인/회원가입, 예매 주체, 마이페이지 예매 내역 조회, 좌석 락 소유자 식별
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "user_id", nullable = false, length = 255)
    private String userId;

    @Column(name = "password", nullable = false, length = 255)
    private String password;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "last_entered_url", columnDefinition = "TEXT")
    private String lastEnteredUrl;

    @Column(name = "last_entered_url_at")
    private Instant lastEnteredUrlAt;

    public static User createForSignup(String userId, String encodedPassword, String name) {
        User user = new User();
        Instant now = KstDateTimeUtils.nowInstant();
        user.setUserId(userId);
        user.setPassword(encodedPassword);
        user.setName(name);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        user.setDeletedAt(null);
        return user;
    }
}
