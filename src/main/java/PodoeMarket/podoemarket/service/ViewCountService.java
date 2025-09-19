package PodoeMarket.podoemarket.service;

import PodoeMarket.podoemarket.common.entity.ProductEntity;
import PodoeMarket.podoemarket.common.repository.ProductRepository;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ViewCountService {
    private final StringRedisTemplate redisTemplate;
    private final ProductRepository productRepo;

    private static final String DELTA_PREFIX = "product:views:delta:";

    // 조회수 증가 (Delta만)
    public void incrementViewForProduct(UUID productId) {
        final String deltaKey = DELTA_PREFIX + productId.toString();
        redisTemplate.opsForValue().increment(deltaKey);
    }

    // 조회수 조회 (DB + Delta)
    public Long getProductViewCount(UUID productId) {
        long base = 0L;
        final ProductEntity product = productRepo.findById(productId);

        if(product != null && product.getViewCount() != null)
            base = product.getViewCount();

        final String deltaKey = DELTA_PREFIX + productId.toString();
        final String deltaStr = redisTemplate.opsForValue().get(deltaKey);
        final long delta = (deltaStr != null) ? Long.parseLong(deltaStr) : 0L;

        return base + delta;
    }

    // 6시간마다 Redis에서 MySQL에 백업
    @Scheduled(cron = "0 0 0,6,12,18 * * *") // 매일 0시, 6시, 12시, 18시에 실행
    @Transactional
    public void flushDeltaToDB() {
        log.info("작품 조회수 델타 동기화 시작");

        RedisConnection conn = redisTemplate.getConnectionFactory().getConnection();
        try (Cursor<byte[]> cursor = conn.scan(
                ScanOptions.scanOptions().match(DELTA_PREFIX + "*").count(500).build())){

            while(cursor.hasNext()) {
                String key = new String(cursor.next(), StandardCharsets.UTF_8);
                UUID productId = UUID.fromString(key.substring(DELTA_PREFIX.length()));
                String deltaStr = redisTemplate.opsForValue().getAndDelete(key);

                if(deltaStr == null)
                    continue;

                long delta;
                try {
                    delta = Long.parseLong(deltaStr);
                } catch (NumberFormatException e) {
                    continue;
                }

                if(delta <=0)
                    continue;

                // DB += delta
                productRepo.incrementViewCount(productId, delta);
            }
        } catch (Exception e) {
            log.error("조회수 동기화 중 오류 발생: ", e);
        } finally {
            conn.close();
        }
    }
}
