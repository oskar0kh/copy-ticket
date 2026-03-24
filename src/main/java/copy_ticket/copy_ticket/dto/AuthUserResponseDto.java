/**
 * GET /api/auth/me
 * 로그인 성공 시, id/userId/name 반환하는 DTO 클래스
 */

package copy_ticket.copy_ticket.dto;

import copy_ticket.copy_ticket.domain.entity.User;

public class AuthUserResponseDto {

    private final Long id;
    private final String userId;
    private final String name;

    public AuthUserResponseDto(Long id, String userId, String name) {
        this.id = id;
        this.userId = userId;
        this.name = name;
    }

    public static AuthUserResponseDto from(User user) {
        return new AuthUserResponseDto(user.getId(), user.getUserId(), user.getName());
    }

    public Long getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }
}
