package PodoeMarket.podoemarket.introduce.service;

import PodoeMarket.podoemarket.introduce.dto.response.StatisticsResponseDTO;
import PodoeMarket.podoemarket.common.entity.type.ProductStatus;
import PodoeMarket.podoemarket.common.repository.ProductRepository;
import PodoeMarket.podoemarket.common.repository.ReviewRepository;
import PodoeMarket.podoemarket.common.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;

@RequiredArgsConstructor
@Slf4j
@Service
@Transactional(readOnly = true)
public class IntroduceService {
    private final UserRepository userRepo;
    private final ProductRepository productRepo;
    private final ReviewRepository reviewRepo;
    private final StringRedisTemplate redisTemplate;

    public StatisticsResponseDTO getStatistics() {
        try {
            return StatisticsResponseDTO.builder()
                    .userCnt(getUserCount())
                    .scriptCnt(getPassedCount())
                    .viewCnt(getViewCount())
                    .reviewCnt(getReviewCount())
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("통계 조회 실패", e);
        }
    }

    // ================= private method =================

    private Long getUserCount() {
        try {
            return userRepo.count();
        } catch (Exception e) {
            throw new RuntimeException("유저 카운트 조회 실패", e);
        }
    }

    private Long getPassedCount() {
        try {
            return productRepo.countAllByChecked(ProductStatus.PASS);
        } catch (Exception e) {
            throw new RuntimeException("등록 작품 카운트 조회 실패", e);
        }
    }

    private Long getViewCount() {
        try {
            long total = productRepo.sumViewCount();

            // Redis Delta 합계
            var conn = redisTemplate.getConnectionFactory().getConnection();
            try(var cursor = conn.scan(
                    ScanOptions.scanOptions().match("product:views:delta:*").count(1000).build())) {
                while (cursor.hasNext()) {
                    String key = new String(cursor.next(), StandardCharsets.UTF_8);
                    String val = redisTemplate.opsForValue().get(key);

                    if(val != null) {
                        try {
                            total += Long.parseLong(val);
                        } catch (NumberFormatException ignore) {}
                    }
                }
            } finally {
                try {
                    conn.close();
                } catch (Exception ignore) {}
            }

            return total;

        } catch (Exception e) {
            throw new RuntimeException("조회수 카운트 조회 실패", e);
        }
    }

    private Long getReviewCount() {
        try {
            return reviewRepo.count();
        } catch (Exception e) {
            throw new RuntimeException("후기 카운트 조회 실패", e);
        }
    }
}
