package PodoeMarket.podoemarket.user.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SignUpRequestDTO {
    private String userId;
    private String email;
    private String password;
    private String confirmPassword;
    private String nickname;
    private String authNum;
}
