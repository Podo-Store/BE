package PodoeMarket.podoemarket.user.service;

import PodoeMarket.podoemarket.common.entity.UserEntity;
import PodoeMarket.podoemarket.common.entity.type.SocialLoginType;
import PodoeMarket.podoemarket.common.repository.UserRepository;
import PodoeMarket.podoemarket.common.security.TokenProvider;
import PodoeMarket.podoemarket.service.MailSendService;
import PodoeMarket.podoemarket.user.dto.OAuthUserDTO;
import PodoeMarket.podoemarket.user.dto.request.SocialSignUpRequestDTO;
import PodoeMarket.podoemarket.user.dto.response.TokenCreateResponseDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.shaded.gson.JsonObject;
import com.nimbusds.jose.shaded.gson.JsonParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.util.List;
import java.util.Random;

@RequiredArgsConstructor
@Slf4j
@Service
@Transactional(readOnly = true)
public class OAuthService {
    private final List<SocialOauth> socialOauthList; // 모든 SocialOauth 구현체가 자동으로 주입됨 (@RequiredArgsConstructor)
    private final TokenProvider tokenProvider;
    private final UserRepository userRepo;
    private final TempAuthService tempAuthService;
    private final MailSendService mailService;

    // 소셜 로그인 요청 URL을 반환하는 메서드
    public String request(SocialLoginType socialLoginType) {
        try {
        SocialOauth socialOauth = findSocialOauthByType(socialLoginType);

        return socialOauth.getOauthRedirectURL();
        } catch (Exception e) {
            throw new RuntimeException("URL 반환 실패", e);
        }
    }

    // 액세스 토큰을 사용하여 사용자 정보를 가져오는 메서드
    public OAuthUserDTO requestUser(SocialLoginType socialLoginType, String code) {
        try {
            // 1. 액세스 토큰을 포함한 JSON 응답 요청
            String infoJson = requestAccessToken(socialLoginType, code);

            // 2. JSON에서 액세스 토큰만 추출
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(infoJson);

            String accessToken = jsonNode.get("access_token") != null ? jsonNode.get("access_token").asText() : null;

            if(accessToken == null)
                throw new RuntimeException("엑세스 토큰 추출 실패");

            // 3. 액세스 토큰을 사용해 사용자 정보 요청
            String userInfo = getUserInfoWithOauth(socialLoginType, accessToken);

            // 4. 사용자 정보를 파싱하여 생성한 User 객체 반환
            return parseUserInfo(userInfo, socialLoginType);
        } catch (Exception e) {
            throw new RuntimeException("사용자 정보 가져오기 실패", e);
        }
    }

    // 소셜 토큰 생성 메서드
    public TokenCreateResponseDTO createToken(final UserEntity signInUser) {
        try {
            if(signInUser == null)
                throw new RuntimeException("사용자를 찾을 수 없습니다.");

            return TokenCreateResponseDTO.builder()
                    .nickname(signInUser.getNickname())
                    .auth(signInUser.isAuth())
                    .accessToken(tokenProvider.createAccessToken(signInUser))
                    .refreshToken(tokenProvider.createRefreshToken(signInUser))
                    .build();
        } catch (Exception e) {
            throw e;
        }
    }

    @Transactional
    public TokenCreateResponseDTO socialSignIn(SocialSignUpRequestDTO dto) {
        try {
            if (!dto.isTermsAgreed())
                throw new RuntimeException("약관 동의 필요");

            OAuthUserDTO oauthUser = tempAuthService.get(dto.getTempCode());

            UserEntity user = UserEntity.builder()
                    .userId(oauthUser.getUserId())
                    .email(oauthUser.getEmail())
                    .nickname(oauthUser.getNickname())
                    .socialLoginType(oauthUser.getSocialLoginType())
                    .build();

            final String userId = user.getUserId();
            final String email = user.getEmail();
            final String nickname = user.getNickname();

            // 아이디
            if(userId == null || userId.isBlank())
                throw new RuntimeException("userId가 올바르지 않음");

            if(userRepo.existsByUserId(userId))
                throw new RuntimeException("이미 존재하는 userId 입니다.");

            // 이메일
            if(email == null || email.isBlank())
                throw new RuntimeException("email이 올바르지 않음");

            if(userRepo.existsByEmail(email))
                throw new RuntimeException("이미 존재하는 email 입니다.");

            // 닉네임
            if(nickname == null || nickname.isBlank())
                throw new RuntimeException("nickname이 올바르지 않음");

            // 닉네임 뒤에 무작위 숫자 6자리를 붙이고 해당 닉네임도 존재하면 다시 설정
            String createNickname = "";
            do {
                createNickname = nickname + createRandomNumber();

            } while (userRepo.existsByNickname(createNickname));

            if(userRepo.existsByNickname(createNickname))
                throw new RuntimeException("이미 존재하는 nickname");

            user.setNickname(createNickname);

            userRepo.save(user);

            mailService.joinSignupEmail(email);
            tempAuthService.remove(dto.getTempCode());

            return createToken(user);
        } catch (Exception e) {
            throw e;
        }
    }

    public UserEntity getUserInfo(final String userId){
        try {
            return userRepo.findByUserId(userId);
        } catch (Exception e){
            throw e;
        }
    }

    // ================= private method =================
    // 인증 코드로 액세스 토큰을 요청하는 메서드
    private String requestAccessToken(SocialLoginType socialLoginType, String code) {
        try {
            SocialOauth socialOauth = findSocialOauthByType(socialLoginType);

            return socialOauth.requestAccessToken(code);
        } catch (Exception e) {
            throw new RuntimeException("엑세스 토큰 요청 실패", e);
        }
    }

    private SocialOauth findSocialOauthByType(SocialLoginType socialLoginType) {
        return socialOauthList.stream()
                .filter(x -> x.type() == socialLoginType)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("알 수 없는 SocialLoginType 입니다."));
    }

    // 소셜 로그인 API에서 사용자 정보를 받아오는 메서드
    private String getUserInfoWithOauth(SocialLoginType socialLoginType, String accessToken) {
        switch (socialLoginType) {
            case GOOGLE:
                return googleApiCall(accessToken);
            case KAKAO:
                return kakaoApiCall(accessToken);
            case NAVER:
                return naverApiCall(accessToken);
                default:
                    throw new IllegalArgumentException("지원되지 않는 소셜 로그인 타입");
        }
    }

    // 구글 API 호출
    private String googleApiCall(String accessToken) {
        try {
            String url = "https://openidconnect.googleapis.com/v1/userinfo";

            URL obj = (new URI(url)).toURL();
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();
            con.setRequestMethod("GET");
            con.setRequestProperty("Authorization", "Bearer " + accessToken);
            con.setRequestProperty("Content-Type", "application/json");

            int responseCode = con.getResponseCode();
            if (responseCode == 200) {
                BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                String inputLine;
                StringBuilder response = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                return response.toString();
            } else {
                throw new RuntimeException("Google API에서 사용자 정보를 가져오는데 실패. response code: " + responseCode);
            }
        } catch (IOException e) {
            throw new RuntimeException("Google API 호출 중 오류 발생", e);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    // 카카오 API 호출
    private String kakaoApiCall(String accessToken) {
        try {
            String url = "https://kapi.kakao.com/v2/user/me";
            URL obj = (new URI(url)).toURL();
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();
            con.setRequestMethod("GET");
            con.setRequestProperty("Authorization", "Bearer " + accessToken);

            int responseCode = con.getResponseCode();
            if (responseCode == 200) {
                BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                String inputLine;
                StringBuilder response = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                return response.toString();
            } else {
                throw new RuntimeException("Kakao API에서 사용자 정보를 가져오는데 실패. response code: " + responseCode);
            }
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException("Kakao API 호출 중 오류 발생", e);
        }
    }

    // 네이버 API 호출
    private String naverApiCall(String accessToken) {
        try {
            String url = "https://openapi.naver.com/v1/nid/me";
            URL obj = (new URI(url)).toURL();
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();
            con.setRequestMethod("GET");
            con.setRequestProperty("Authorization", "Bearer " + accessToken);

            int responseCode = con.getResponseCode();
            if (responseCode == 200) {
                BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                String inputLine;
                StringBuilder response = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();
                return response.toString();
            } else {
                throw new RuntimeException("Naver API에서 사용자 정보를 가져오는데 실패. response code: " + responseCode);
            }
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException("Naver API 호출 중 오류 발생", e);
        }
    }

    // 사용자 정보를 파싱하여 User 객체 생성
    private OAuthUserDTO parseUserInfo(String userInfo, SocialLoginType socialLoginType) {
        JsonObject jsonObject = JsonParser.parseString(userInfo).getAsJsonObject();

        String userId = "";
        String nickname = "";
        String email = "";
        SocialLoginType type = null;

        if (socialLoginType == SocialLoginType.GOOGLE) {
            userId = jsonObject.get("sub").getAsString();
            nickname = jsonObject.get("name").getAsString();
            email = jsonObject.get("email").getAsString();
            type = SocialLoginType.GOOGLE;
        } else if (socialLoginType == SocialLoginType.KAKAO) {
            userId = jsonObject.get("id").getAsString();
            nickname = jsonObject.getAsJsonObject("properties").get("nickname").getAsString();
            email = jsonObject.getAsJsonObject("kakao_account").get("email").getAsString();
            type= SocialLoginType.KAKAO;
        } else if (socialLoginType == SocialLoginType.NAVER) {
            JsonObject response = jsonObject.getAsJsonObject("response");
            userId = response.get("id").getAsString();
            nickname = response.get("nickname").getAsString();
            email = response.get("email").getAsString();
            type = SocialLoginType.NAVER;
        }

        return OAuthUserDTO.builder()
                .userId(userId)
                .nickname(nickname)
                .email(email)
                .socialLoginType(type)
                .build();
    }

    // 랜덤으로 6자리 숫자 생성
    private String createRandomNumber() {
        Random r = new Random();
        StringBuilder randomNumber = new StringBuilder();

        for(int i = 0; i < 6; i++)
            randomNumber.append(r.nextInt(9) + 1);

        return randomNumber.toString();
    }
}
