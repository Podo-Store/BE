package PodoeMarket.podoemarket.repository;

import PodoeMarket.podoemarket.entity.OrdersEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<OrdersEntity, Long> {
}
