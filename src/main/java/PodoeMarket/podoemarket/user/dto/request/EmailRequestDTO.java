package PodoeMarket.podoemarket.user.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EmailRequestDTO {
    @NotEmpty(message = "이메일을 입력해 주세요")
    private String email;
    private Boolean flag;
    private String userId;
}
