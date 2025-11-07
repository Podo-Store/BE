package PodoeMarket.podoemarket.user.controller;

import PodoeMarket.podoemarket.common.entity.UserEntity;
import PodoeMarket.podoemarket.common.entity.type.SocialLoginType;
import PodoeMarket.podoemarket.common.dto.ResponseDTO;
import PodoeMarket.podoemarket.service.MailSendService;
import PodoeMarket.podoemarket.user.dto.response.TokenCreateResponseDTO;
import PodoeMarket.podoemarket.user.service.OAuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@Slf4j
@RequestMapping("/auth")
public class OauthController {
    private final OAuthService oauthService;
    private final MailSendService mailService;

    @GetMapping(value = "/{socialLoginType}")
    public ResponseEntity<?> socialLoginType(@PathVariable(name = "socialLoginType") SocialLoginType socialLoginType) {
        String redirectURL = oauthService.request(socialLoginType);

        return ResponseEntity.ok().body(redirectURL);
    }

    @GetMapping(value = "/{socialLoginType}/callback")
    public ResponseEntity<?> callback(@PathVariable(name = "socialLoginType") SocialLoginType socialLoginType,
                                      @RequestParam(name = "code") String code) {
        try {
            final UserEntity user = oauthService.requestUser(socialLoginType, code);

            if (oauthService.checkUserId(user.getUserId())) {
                final TokenCreateResponseDTO resDTO = oauthService.socialSignIn(user);

                mailService.joinSignupEmail(user.getEmail());

                return ResponseEntity.ok().body(resDTO);
            } else {
                // 새 사용자는 저장 -> 동일 이메일 확인 필요
                oauthService.create(user);

                return ResponseEntity.ok().body(true);
            }
        } catch(Exception e) {
            ResponseDTO resDTO = ResponseDTO.builder().error(e.getMessage()).build();
            return ResponseEntity.badRequest().body(resDTO);
        }
    }
}
