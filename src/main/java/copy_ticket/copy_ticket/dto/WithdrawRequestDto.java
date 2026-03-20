package copy_ticket.copy_ticket.dto;

import jakarta.validation.constraints.AssertTrue;

public class WithdrawRequestDto {

    @AssertTrue(message = "회원 탈퇴 확인이 필요합니다.")
    private boolean confirmed;

    public boolean isConfirmed() {
        return confirmed;
    }

    public void setConfirmed(boolean confirmed) {
        this.confirmed = confirmed;
    }
}