/**
 * GET /api/auth/me
 * 로그인 성공 시, idx/id/name 반환하는 DTO 클래스
 */

package copy_ticket.copy_ticket.dto;

import copy_ticket.copy_ticket.domain.entity.User;

public class AuthUserResponseDto {

    private final Long idx;
    private final String id;
    private final String name;

    public AuthUserResponseDto(Long idx, String id, String name) {
        this.idx = idx;
        this.id = id;
        this.name = name;
    }

    public static AuthUserResponseDto from(User user) {
        return new AuthUserResponseDto(user.getIdx(), user.getId(), user.getName());
    }

    public Long getIdx() {
        return idx;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
