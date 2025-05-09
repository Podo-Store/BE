package PodoeMarket.podoemarket.service;

import PodoeMarket.podoemarket.common.entity.ProductEntity;
import PodoeMarket.podoemarket.common.repository.ProductRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ViewCountService {
    private final StringRedisTemplate redisTemplate;
    private final ProductRepository productRepo;

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

            log.info("조회수 동기화 및 데이터 정리 성공");
        } catch (Exception e) {
            log.error("작품 조회수 동기화 중 오류 발생: ", e);
        }
    }
}
