package PodoeMarket.podoemarket.repository;

import PodoeMarket.podoemarket.entity.OrderItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OrderItemRepository extends JpaRepository<OrderItemEntity, Long> {
    OrderItemEntity findByProductId(UUID id);
    Boolean existsByProductId(UUID id);
}
