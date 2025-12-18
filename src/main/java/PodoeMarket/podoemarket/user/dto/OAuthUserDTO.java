package PodoeMarket.podoemarket.user.dto;

import PodoeMarket.podoemarket.common.entity.type.SocialLoginType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OAuthUserDTO {
    private String userId;
    private String email;
    private String nickname;
    private SocialLoginType socialLoginType;
}
