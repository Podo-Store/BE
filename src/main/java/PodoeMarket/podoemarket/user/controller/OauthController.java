package PodoeMarket.podoemarket.user.controller;

import PodoeMarket.podoemarket.common.entity.UserEntity;
import PodoeMarket.podoemarket.common.entity.type.SocialLoginType;
import PodoeMarket.podoemarket.common.security.TokenProvider;
import PodoeMarket.podoemarket.dto.response.ResponseDTO;
import PodoeMarket.podoemarket.service.MailSendService;
import PodoeMarket.podoemarket.user.dto.response.SignInResponseDTO;
import PodoeMarket.podoemarket.user.service.OAuthService;
import PodoeMarket.podoemarket.user.service.UserService;
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
    private final UserService userService;
    private final MailSendService mailService;
    private final TokenProvider tokenProvider;

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

            if (userService.checkUserId(user.getUserId())) {
                final UserEntity signInUser = userService.getByUserId(user.getUserId());

                final SignInResponseDTO resUserDTO = SignInResponseDTO.builder()
                        .nickname(signInUser.getNickname())
                        .auth(signInUser.isAuth())
                        .accessToken(tokenProvider.createAccessToken(user))
                        .refreshToken(tokenProvider.createRefreshToken(user))
                        .build();

                mailService.joinSignupEmail(signInUser.getEmail());

                return ResponseEntity.ok().body(resUserDTO);
            } else {
                // 새 사용자는 저장
                oauthService.create(user);

                return ResponseEntity.ok().body(true);
            }
        } catch(Exception e) {
            ResponseDTO resDTO = ResponseDTO.builder().error(e.getMessage()).build();
            return ResponseEntity.badRequest().body(resDTO);
        }
    }
}
