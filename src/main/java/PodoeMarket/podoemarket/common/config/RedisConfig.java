package PodoeMarket.podoemarket.common.config;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.SocketOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;

@Configuration
public class RedisConfig {
    // 배포용
    @Value("${spring.data.redis.host}")
    private String redisHost;
    @Value("${spring.data.redis.port}")
    private int redisPort;
    @Value("${spring.data.redis.timeout:300ms}")
    private Duration commandTimeout;

    @Bean
    public LettuceConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration serverConfig = new RedisStandaloneConfiguration(redisHost, redisPort);

        // 연결 단계 블로킹도 command timeout과 동일하게 제한
        ClientOptions clientOptions = ClientOptions.builder()
                .socketOptions(SocketOptions.builder()
                        .connectTimeout(commandTimeout)
                        .build())
                .build();

        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
                .clientOptions(clientOptions)
                .commandTimeout(commandTimeout) // 기본값 60s -> 명시적으로 300ms
                .build();

        LettuceConnectionFactory factory = new LettuceConnectionFactory(serverConfig, clientConfig);
        factory.setValidateConnection(true);
        return factory;
    }

    @Bean
    public RedisTemplate<String, String> redisTemplate() {
        StringRedisTemplate stringRedisTemplate = new StringRedisTemplate();
        stringRedisTemplate.setConnectionFactory(redisConnectionFactory());
        return stringRedisTemplate;
    }

    // 로컬용
//    @Bean
//    public RedisConnectionFactory redisConnectionFactory() {
//        return new LettuceConnectionFactory();
//    }
}
