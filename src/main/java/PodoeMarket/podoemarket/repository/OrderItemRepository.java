package PodoeMarket.podoemarket.repository;

import PodoeMarket.podoemarket.entity.OrderItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OrderItemRepository extends JpaRepository<OrderItemEntity, Long> {
    Boolean existsByProductIdAndUserId(UUID productId, UUID userId);

    List<OrderItemEntity> findByProductIdAndUserId(UUID productId, UUID userId);

    List<OrderItemEntity> findByOrderId(Long id);

    OrderItemEntity findById(UUID id);
}
