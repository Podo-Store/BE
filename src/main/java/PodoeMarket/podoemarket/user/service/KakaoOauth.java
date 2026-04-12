package PodoeMarket.podoemarket.user.service;

import PodoeMarket.podoemarket.common.config.SnsProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class KakaoOauth implements SocialOauth {
    private final SnsProperties snsProperties;

    @Override
    public String getOauthRedirectURL() {
        Map<String, Object> params = new HashMap<>();
        params.put("response_type", "code");
        params.put("client_id", snsProperties.getKakao().getClientId());
        params.put("redirect_uri", snsProperties.getKakao().getCallbackUrl());

        String parameterString = params.entrySet().stream()
                .map(x -> x.getKey() + "=" + x.getValue())
                .collect(Collectors.joining("&"));

        return snsProperties.getKakao().getUrl() + "?" + parameterString;
    }

    @Override
    public String requestAccessToken(String code) {
        RestTemplate restTemplate = new RestTemplate();

        // Header 설정 (Content-Type을 application/x-www-form-urlencoded로 설정)
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        // 파라미터 설정
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("code", code);
        params.add("client_id", snsProperties.getKakao().getClientId());
        params.add("client_secret", snsProperties.getKakao().getClientSecret());
        params.add("redirect_uri", snsProperties.getKakao().getCallbackUrl());
        params.add("grant_type", "authorization_code");

        // HttpEntity 생성
        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(params, headers);

        // POST 요청 전송
        ResponseEntity<String> responseEntity =
                restTemplate.postForEntity(snsProperties.getKakao().getTokenUrl(), requestEntity, String.class);

        if (responseEntity.getStatusCode() == HttpStatus.OK)
            return responseEntity.getBody();

        return "카카오 로그인 요청 처리 실패";
    }
}
