package PodoeMarket.podoemarket.user.controller;

import PodoeMarket.podoemarket.common.dto.ResponseDTO;
import PodoeMarket.podoemarket.common.entity.UserEntity;
import PodoeMarket.podoemarket.common.entity.type.SocialLoginType;
import PodoeMarket.podoemarket.user.dto.OAuthUserDTO;
import PodoeMarket.podoemarket.user.dto.request.SocialSignUpRequestDTO;
import PodoeMarket.podoemarket.user.dto.response.TokenCreateResponseDTO;
import PodoeMarket.podoemarket.user.service.OAuthService;
import PodoeMarket.podoemarket.user.service.TempAuthService;
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
    private final TempAuthService tempAuthService;

    @GetMapping(value = "/{socialLoginType}")
    public ResponseEntity<?> socialLoginType(@PathVariable(name = "socialLoginType") SocialLoginType socialLoginType) {
        try {
            String redirectURL = oauthService.request(socialLoginType);

            return ResponseEntity.ok().body(redirectURL);
        } catch(Exception e) {
            ResponseDTO resDTO = ResponseDTO.builder().error(e.getMessage()).build();
            return ResponseEntity.badRequest().body(resDTO);
        }
    }

    // 배포 시에 리디렉트 url 변경 필요 - Oauth 서버가 접근하는 url 프론트에서 호출 X
    @GetMapping(value = "/{socialLoginType}/callback")
    public void callback(@PathVariable(name = "socialLoginType") SocialLoginType socialLoginType,
                         @RequestParam(name = "code") String code,
                         HttpServletResponse response) throws IOException {
        try {
            final OAuthUserDTO oauthUser = oauthService.requestUser(socialLoginType, code);
            UserEntity dbUser = oauthService.getUserInfo(oauthUser.getUserId());

            if(dbUser != null) {
                // 기존 회원 -> 로그인
            final TokenCreateResponseDTO token = oauthService.createToken(dbUser);

            // 프론트로 리디렉트
                response.sendRedirect(
                        "http://localhost:3000/auth/callback" +
//                                "https://www.podo-store.com/auth/callback" +
                                "?result=LOGIN" +
                                "&accessToken=" + token.getAccessToken() +
                                "&refreshToken=" + token.getRefreshToken() +
                                "&nickname=" +  URLEncoder.encode(token.getNickname(), StandardCharsets.UTF_8)
                );

                return;
            }

            // 신규 유저
            String tempCode = tempAuthService.store(oauthUser);

            response.sendRedirect(
                    "http://localhost:3000/auth/callback" +
//                            "https://www.podo-store.com/auth/callback" +
                            "?result=REQUIRE_TERMS" +
                            "&tempCode=" + tempCode
            );
        } catch(Exception e) {
            e.printStackTrace();
            response.sendRedirect("http://localhost:3000/auth/callback?error=" + URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8));
//            response.sendRedirect("https://www.podo-store.com/auth/callback?error=" + URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8));
        }
    }

    @PostMapping("/social/signin")
    public ResponseEntity<?> signin(@RequestBody SocialSignUpRequestDTO request) {
        try {
            TokenCreateResponseDTO token = oauthService.socialSignIn(request);

            return ResponseEntity.ok().body(token);
        } catch(Exception e) {
            ResponseDTO resDTO = ResponseDTO.builder().error(e.getMessage()).build();
            return ResponseEntity.badRequest().body(resDTO);
        }
    }
}
