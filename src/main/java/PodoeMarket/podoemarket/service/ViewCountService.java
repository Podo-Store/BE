package PodoeMarket.podoemarket.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ViewCountService {
    private final StringRedisTemplate redisTemplate;

     // 로그인한 사용자의 조회수 증가 처리
    public void incrementViewForLogged(UUID userId, UUID productId) {
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String key = String.format("user:%s:views:%s", userId.toString(), today);

        // 이미 조회한 상품인지 확인 (NullPointerException 방지)
        Long result = redisTemplate.opsForSet().add(key, productId.toString());
        boolean isView = result != null && result == 1;

        // 키 만료 시간 설정(당일 자정까지)
        LocalDateTime midnight = LocalDate.now().plusDays(1).atStartOfDay();
        Duration timeUntilMidnight = Duration.between(LocalDateTime.now(), midnight);
        redisTemplate.expire(key, timeUntilMidnight);

        if(isView) {
            // 조회수 증가
            String viewCountkey = "product:views:" + productId;
            redisTemplate.opsForValue().increment(viewCountkey);
        }
    }

    // 비로그인 사용자의 조회 확인 (쿠키는 컨트롤러에서 처리)
    public void incrementViewForProduct(UUID productId) {
        String viewCountKey = "product:views:" + productId.toString();
        redisTemplate.opsForValue().increment(viewCountKey);
    }

    // 상품의 현재 조회수 조회
    public Long getProductViewCount(UUID productId) {
        String viewCountKey = "product:views:" + productId.toString();
        String count = redisTemplate.opsForValue().get(viewCountKey);

        return count != null ? Long.parseLong(count) : 0L;
    }

    // MySQL에서 조회수를 가져와 Redis에 설정
    public void syncViewCountFromDB(UUID productId, Long viewCount) {
        String viewCountKey = "product:views:" + productId.toString();
        redisTemplate.opsForValue().set(viewCountKey, viewCountKey);
    }
}
