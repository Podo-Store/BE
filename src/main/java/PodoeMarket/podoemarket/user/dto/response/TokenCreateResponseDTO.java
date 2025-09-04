package PodoeMarket.podoemarket.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenCreateResponseDTO {
    private String nickname;
    private Boolean auth;
    private String accessToken; // jwt 저장공간
    private String refreshToken; // jwt 저장공간
}
