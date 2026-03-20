/**
 * POST /api/login
 * 로그인 요청 시, id/password를 받는 DTO 클래스
 */

package copy_ticket.copy_ticket.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class LoginRequestDto {

    @NotBlank(message = "아이디를 입력해 주세요.")
    @Size(max = 20, message = "아이디는 20자 이하여야 합니다.")
    private String id;

    @NotBlank(message = "비밀번호를 입력해 주세요.")
    @Size(min = 4, max = 20, message = "비밀번호는 4자 이상 20자 이하로 입력해 주세요.")
    private String password;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
