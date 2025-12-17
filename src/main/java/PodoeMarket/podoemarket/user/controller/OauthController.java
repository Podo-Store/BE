package PodoeMarket.podoemarket.user.controller;

import PodoeMarket.podoemarket.common.entity.UserEntity;
import PodoeMarket.podoemarket.common.entity.type.SocialLoginType;
import PodoeMarket.podoemarket.service.MailSendService;
import PodoeMarket.podoemarket.user.dto.response.TokenCreateResponseDTO;
import PodoeMarket.podoemarket.user.service.OAuthService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

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

    // 배포 시에 리디렉트 url 변경 필요
    @GetMapping(value = "/{socialLoginType}/callback")
    public void callback(@PathVariable(name = "socialLoginType") SocialLoginType socialLoginType,
                                      @RequestParam(name = "code") String code,
                                      HttpServletResponse response) throws IOException {
        try {
            final UserEntity oauthUser = oauthService.requestUser(socialLoginType, code);
            UserEntity dbUser = oauthService.getUserInfo(oauthUser.getUserId());
            boolean isNewUser = false;

            if (dbUser == null) {
                isNewUser = true;
                // 새 사용자는 저장
                oauthService.create(oauthUser);

                dbUser = oauthService.getUserInfo(oauthUser.getUserId());
                mailService.joinSignupEmail(dbUser.getEmail());
            }

            // 토큰 발급
            final TokenCreateResponseDTO resDTO = oauthService.socialSignIn(dbUser);

            // 프론트로 리디렉트(JWT와 닉네임 등 전달)
            String redirectUrl = String.format(
                    "http://localhost:3000/auth/callback?accessToken=%s&refreshToken=%s&nickname=%s&isNewUser=%s",
                    resDTO.getAccessToken(),
                    resDTO.getRefreshToken(),
                    URLEncoder.encode(resDTO.getNickname(), StandardCharsets.UTF_8),
                    isNewUser
            );
//                String redirectUrl = String.format(
//                        "https://www.podo-store.com/auth/callback?accessToken=%s&refreshToken=%s&nickname=%s",
//                        resDTO.getAccessToken(),
//                        resDTO.getRefreshToken(),
//                        URLEncoder.encode(resDTO.getNickname(), StandardCharsets.UTF_8)
//                );

            response.sendRedirect(redirectUrl);
        } catch(Exception e) {
            e.printStackTrace();
            response.sendRedirect("http://localhost:3000/auth/callback?error=" + URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8));
//            response.sendRedirect("https://www.podo-store.com/auth/callback?error=" + URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8));
        }
    }
}
