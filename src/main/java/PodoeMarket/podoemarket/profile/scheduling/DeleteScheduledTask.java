package PodoeMarket.podoemarket.profile.scheduling;

import PodoeMarket.podoemarket.common.entity.ProductEntity;
import PodoeMarket.podoemarket.common.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeleteScheduledTask {
    private final ProductRepository productRepo;

    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void deleteScript() {
        LocalDateTime today = LocalDate.now(ZoneId.of("Asia/Seoul")).atStartOfDay();

        // isDelete가 true, updatedAt이 1년 이후 일 때
        List<ProductEntity> deletedProducts = productRepo.findAllByIsDeleteAndUpdatedAt(true, today.plusYears(1));

        productRepo.deleteAll(deletedProducts);

        log.info("작품 자동 삭제 작업이 완료되었습니다.");
    }
}
