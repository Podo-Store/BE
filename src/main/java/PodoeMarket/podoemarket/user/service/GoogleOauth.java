package PodoeMarket.podoemarket.user.service;

import PodoeMarket.podoemarket.common.config.SnsProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class GoogleOauth implements SocialOauth {
    private final SnsProperties snsProperties;

    @Override
    public String getOauthRedirectURL() {
        Map<String, Object> params = new HashMap<>();
        params.put("scope", "email profile");
        params.put("response_type", "code");
        params.put("client_id", snsProperties.getGoogle().getUrl());
        params.put("redirect_uri", snsProperties.getGoogle().getCallbackUrl());

        String parameterString = params.entrySet().stream()
                .map(x -> x.getKey() + "=" + x.getValue())
                .collect(Collectors.joining("&"));

        return snsProperties.getGoogle().getUrl() + "?" + parameterString;
    }

    @Override
    public String requestAccessToken(String code) {
        RestTemplate restTemplate = new RestTemplate();

        // 파라미터 설정
        Map<String, Object> params = new HashMap<>();
        params.put("code", code);
        params.put("client_id", snsProperties.getGoogle().getClientId());
        params.put("client_secret", snsProperties.getGoogle().getClientSecret());
        params.put("redirect_uri", snsProperties.getGoogle().getCallbackUrl());
        params.put("grant_type", "authorization_code");

        // POST 요청 전송
        ResponseEntity<String> responseEntity = restTemplate.postForEntity(snsProperties.getGoogle().getTokenUrl(), params, String.class);

        if (responseEntity.getStatusCode() == HttpStatus.OK)
            return responseEntity.getBody();

        return "구글 로그인 요청 처리 실패";
    }
}
