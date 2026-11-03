package PodoeMarket.podoemarket.service;

import PodoeMarket.podoemarket.common.entity.UserEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccessLogService {
    private final StringRedisTemplate redisTemplate;
    private final LogSaveService logSaveService;

    private static final String ACCESS_PREFIX = "access:last:";
    private static final Duration ACCESS_WINDOW = Duration.ofMinutes(30);

    // 로그인 - 항상 기록하고 30분 구간을 새로 시작 (어드민 제외)
    public void recordLogin(UserEntity user) {
        if(user.isAuth())
            return;

        try {
            redisTemplate.opsForValue().set(ACCESS_PREFIX + user.getId(), "1", ACCESS_WINDOW);
        } catch (Exception e) {
            log.warn("Access window reset skipped (Redis unavailable)");
        }

        logSaveService.saveAccessLog(user.getId(), now());
    }

    // 인증된 요청 - 마지막 기록 후 30분이 지났을 때만 기록 (고정 30분, 어드민 제외)
    // 매 요청마다 호출되므로 Redis 지연이 응답에 영향을 주지 않도록 비동기 처리
    @Async
    public void recordAccess(UserEntity user) {
        if(user.isAuth())
            return;

        try {
            Boolean isNewAccess = redisTemplate.opsForValue().setIfAbsent(ACCESS_PREFIX + user.getId(), "1", ACCESS_WINDOW);

            if(Boolean.TRUE.equals(isNewAccess))
                logSaveService.saveAccessLog(user.getId(), now());
        } catch (Exception e) {
            log.warn("Access log skipped (Redis unavailable)");
        }
    }

    private LocalDateTime now() {
        return LocalDateTime.now(ZoneId.of("Asia/Seoul"));
    }
}
