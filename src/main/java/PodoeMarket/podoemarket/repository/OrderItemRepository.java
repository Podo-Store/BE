package PodoeMarket.podoemarket.repository;

import PodoeMarket.podoemarket.entity.OrderItemEntity;
import PodoeMarket.podoemarket.entity.ProductEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OrderItemRepository extends JpaRepository<OrderItemEntity, Long> {
    Boolean existsByProductIdAndUserId(UUID productId, UUID userId);

    List<OrderItemEntity> findByProductIdAndUserId(UUID productId, UUID userId);

    List<OrderItemEntity> findByOrderId(Long id);

    List<OrderItemEntity> findAllByUserIdAndScript(UUID id, boolean script);

//    List<OrderItemEntity> findAllByUserIdAndPerformance(UUID id, boolean performance);
//
//    List<OrderItemEntity> findByPerformanceAndProduct(boolean performance, ProductEntity product);

    OrderItemEntity findById(UUID id);
}
