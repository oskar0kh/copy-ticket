package copy_ticket.copy_ticket.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class SignupRequestDto {

    @NotBlank(message = "아이디를 입력해 주세요.")
    @Size(max = 20, message = "아이디는 20자 이하여야 합니다.")
    private String id;

    @NotBlank(message = "비밀번호를 입력해 주세요.")
    @Size(min = 4, max = 20, message = "비밀번호는 4자 이상 20자 이하로 입력해 주세요.")
    private String password;

    @NotBlank(message = "이름을 입력해 주세요.")
    @Size(max = 20, message = "이름은 20자 이하여야 합니다.")
    private String name;

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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
