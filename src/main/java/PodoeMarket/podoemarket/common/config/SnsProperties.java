package PodoeMarket.podoemarket.common.config;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Component
@ConfigurationProperties("sns")
public class SnsProperties {
    private Provider google;
    private Provider kakao;
    private Provider naver;

    @Getter
    public static class Provider {
        private String url;
        private String clientId;
        private String clientSecret;
        private String callbackUrl;
        private String tokenUrl;
        private String userUrl;
    }
}
