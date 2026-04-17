package copy_ticket.copy_ticket.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 대기열 참가용 Request DTO

@Getter
@NoArgsConstructor
public class PublicQueueJoinRequestDto {

    @NotNull(message = "roundId는 필수입니다.")
    @Min(value = 1, message = "roundId는 1 이상이어야 합니다.")
    private Integer roundId;
}
