package PodoeMarket.podoemarket.service;

import PodoeMarket.podoemarket.common.entity.ProductEntity;
import PodoeMarket.podoemarket.common.repository.ProductRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ViewCountService {
    private final StringRedisTemplate redisTemplate;
    private final ProductRepository productRepo;

     // 로그인한 사용자의 조회수 증가 처리
    public void incrementViewForLogged(UUID userId, UUID productId) {
        final String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        final String key = String.format("user:%s:views:%s", userId.toString(), today);

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
        final String viewCountKey = "product:views:" + productId.toString();
        redisTemplate.opsForValue().increment(viewCountKey);
    }

    // 상품의 현재 조회수 조회
    public Long getProductViewCount(UUID productId) {
        final String viewCountKey = "product:views:" + productId.toString();
        final String count = redisTemplate.opsForValue().get(viewCountKey);

        // Redis에 데이터가 없으면 DB에서 가져옴
        if (count == null) {
            final ProductEntity product = productRepo.findById(productId);

            if (product != null) {
                final Long dbViewCount = product.getViewCount();
                syncViewCountFromDB(productId, dbViewCount);
                return dbViewCount;
            }
            return 0L;
        }

        return Long.parseLong(count);
    }

    // MySQL에서 조회수를 가져와 Redis에 설정
    public void syncViewCountFromDB(UUID productId, Long viewCount) {
        final String viewCountKey = "product:views:" + productId.toString();
        redisTemplate.opsForValue().set(viewCountKey, viewCount.toString());
    }

    // 6시간마다 Redis에서 MySQL에 백업
    @Scheduled(cron = "0 0 0,6,12,18 * * *") // 매일 0시, 6시, 12시, 18시에 실행
    @Transactional
    public void syncViewCountFromRedis() {
        log.info("작품 조회수 동기화 시작");
        try {
            // 모든 상품 조회수 키 패턴
            Set<String> keys = redisTemplate.keys("product:views:*");

            if (keys == null || keys.isEmpty())
                log.info("동기화할 데이터가 없습니다.");

            for (String key : keys) {
                // 상품 ID 추출
                final String productIdStr = key.replace("product:views:", "");
                final UUID productId = UUID.fromString(productIdStr);

                // Redis에서 조회수 가져오기
                final String viewCountStr = redisTemplate.opsForValue().get(key);
                if (viewCountStr != null) {
                    final Long viewCount = Long.valueOf(viewCountStr);

                    // MySQL에 업데이트
                    ProductEntity product = productRepo.findById(productId);
                    if (product != null) {
                        product.setViewCount(viewCount);
                        productRepo.save(product);

                        // MySQL 업데이트 후 Redis 데이터 삭제
                        redisTemplate.delete(key);
                    }
                }
            }

            // 오래된 사용자 조회 기록 삭제 (30일 이상 된 기록)
            Set<String> oldUserViewKeys = redisTemplate.keys("user:*:views:*");
            if (oldUserViewKeys != null && !oldUserViewKeys.isEmpty()) {
                for (String key : oldUserViewKeys) {
                    String[] parts = key.split(":");

                    if (parts.length >= 4) {
                        String dateStr = parts[3];
                        try {
                            LocalDate keyDate = LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyyMMdd"));

                            if (keyDate.isBefore(LocalDate.now().minusDays(30)))
                                redisTemplate.delete(key);
                        } catch (Exception e) {
                            log.warn("날짜 파싱 오류: {}", key);
                        }
                    }
                }
            }

            log.info("조회수 동기화 및 데이터 정리 성공");
        } catch (Exception e) {
            log.error("작품 조회수 동기화 중 오류 발생: ", e);
        }
    }
}
