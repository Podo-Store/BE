package PodoeMarket.podoemarket.user.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SocialSignUpRequestDTO {
    /**
     * OAuth 콜백에서 발급된 임시 인증 코드
     * (Redis / Cache 에 저장된 OAuthUser를 식별)
     */
//    @NotBlank
    private String tempCode;

    /**
     * 약관 동의 여부
     * 반드시 true여야 회원가입 진행
     */
//    @AssertTrue(message = "약관 동의가 필요합니다.")
    private boolean termsAgreed;
}
