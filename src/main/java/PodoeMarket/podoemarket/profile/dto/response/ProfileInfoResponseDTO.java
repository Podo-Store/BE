package PodoeMarket.podoemarket.profile.dto.response;

import PodoeMarket.podoemarket.common.entity.type.SocialLoginType;
import PodoeMarket.podoemarket.common.entity.type.StageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileInfoResponseDTO {
    private UUID id;
    private String userId;
    private String email;
    private SocialLoginType socialLoginType;
    private String nickname;
    private StageType stageType;
    private String accountNumber;
}
