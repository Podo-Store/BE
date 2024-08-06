package PodoeMarket.podoemarket.repository;

import PodoeMarket.podoemarket.entity.OrdersEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<OrdersEntity, Long> {
    List<OrdersEntity> findAllByUserId(UUID id);
}
