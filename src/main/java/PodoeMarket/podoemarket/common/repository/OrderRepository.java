package PodoeMarket.podoemarket.common.repository;

import PodoeMarket.podoemarket.common.entity.OrdersEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<OrdersEntity, Long> {
    OrdersEntity findByOrderId(Long orderId);
}
