package PodoeMarket.podoemarket.order.scheduling;

import PodoeMarket.podoemarket.common.entity.type.OrderStatus;
import PodoeMarket.podoemarket.common.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderScheduledTask {
    private final OrderRepository orderRepo;

    @Scheduled(fixedDelay = 60_000) // 1분마다 실행
    @Transactional
    public void deleteExpiredPendingOrders() {

        LocalDateTime expiredAt = LocalDateTime.now().minusMinutes(5);

        int deletedCount = orderRepo.deleteExpiredOrders(OrderStatus.PENDING, expiredAt);

        if (deletedCount > 0) {
            log.info(
                    "[OrderScheduledTask] deleted {} expired PENDING orders (before {})",
                    deletedCount,
                    expiredAt
            );
        }
    }
}
