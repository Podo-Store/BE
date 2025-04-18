package PodoeMarket.podoemarket.user.service;

import PodoeMarket.podoemarket.common.entity.UserEntity;
import PodoeMarket.podoemarket.common.entity.type.SocialLoginType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.shaded.gson.JsonObject;
import com.nimbusds.jose.shaded.gson.JsonParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.util.List;

@RequiredArgsConstructor
@Slf4j
@Service
public class OAuthService {
    private final List<SocialOauth> socialOauthList; // 모든 SocialOauth 구현체가 자동으로 주입됨 (@RequiredArgsConstructor)

    // 소셜 로그인 요청 URL을 반환하는 메서드
    public String request(SocialLoginType socialLoginType) {
        SocialOauth socialOauth = findSocialOauthByType(socialLoginType);
        return socialOauth.getOauthRedirectURL();
    }

    // 인증 코드로 액세스 토큰을 요청하는 메서드
    public String requestAccessToken(SocialLoginType socialLoginType, String code) {
        SocialOauth socialOauth = findSocialOauthByType(socialLoginType);
        return socialOauth.requestAccessToken(code);
    }

    // 액세스 토큰을 사용하여 사용자 정보를 가져오는 메서드
    public UserEntity requestUser(SocialLoginType socialLoginType, String code) {
        // 1. 액세스 토큰을 포함한 JSON 응답 요청
        String infoJson = requestAccessToken(socialLoginType, code);

        // 2. JSON에서 액세스 토큰만 추출
        String accessToken = extractAccessToken(infoJson);

        if(accessToken == null)
            throw new RuntimeException("Failed to extract access token");

        // 3. 액세스 토큰을 사용해 사용자 정보 요청
        String userInfo = getUserInfoWithOauth(socialLoginType, accessToken);

        // 4. 사용자 정보를 파싱하여 생성한 User 객체 반환
        return parseUserInfo(userInfo, socialLoginType);
    }

    private SocialOauth findSocialOauthByType(SocialLoginType socialLoginType) {
        return socialOauthList.stream()
                .filter(x -> x.type() == socialLoginType)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("알 수 없는 SocialLoginType 입니다."));
    }

    // JSON에서 액세스 토큰만 추출하는 메서드
    private String extractAccessToken(String infoJson) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(infoJson);

            return jsonNode.get("access_token") != null ? jsonNode.get("access_token").asText() : null;
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return null;
        }
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
    public String googleApiCall(String accessToken) {
        try {
            String encodedAccessToken = URLEncoder.encode(accessToken, "UTF-8");
            log.debug("Encoded access token: {}", encodedAccessToken);

            String url = "https://www.googleapis.com/oauth2/v3/userinfo?access_token=" + encodedAccessToken;
            log.debug("Google API URL: {}", url);

            URL obj = (new URI(url)).toURL();
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();
            con.setRequestMethod("GET");
            con.setRequestProperty("Content-Type", "application/json");

            int responseCode = con.getResponseCode();
            if (responseCode == 200) {
                BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                String inputLine;
                StringBuffer response = new StringBuffer();
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                return response.toString();
            } else {
                throw new RuntimeException("Google API에서 사용자 정보를 가져오는데 실패. response code: " + responseCode);
            }
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException("Google API 호출 중 오류 발생", e);
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
                StringBuffer response = new StringBuffer();
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
                StringBuffer response = new StringBuffer();
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
    private UserEntity parseUserInfo(String userInfo, SocialLoginType socialLoginType) {
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
            nickname = response.get("name").getAsString();
            email = response.get("email").getAsString();
            type = SocialLoginType.NAVER;
        }

        return UserEntity.builder()
                .userId(userId)
                .nickname(nickname)
                .email(email)
                .socialLoginType(type)
                .build();
    }
}
