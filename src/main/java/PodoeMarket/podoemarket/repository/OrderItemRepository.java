package PodoeMarket.podoemarket.repository;

import PodoeMarket.podoemarket.entity.OrderItemEntity;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface OrderItemRepository extends JpaRepository<OrderItemEntity, Long> {
    Boolean existsByProductIdAndUserId(UUID productId, UUID userId);

    List<OrderItemEntity> findByProductIdAndUserId(UUID productId, UUID userId);

    List<OrderItemEntity> findByOrderId(Long id);

    List<OrderItemEntity> findAllByUserIdAndScript(UUID id, boolean script, Sort sort);

    List<OrderItemEntity> findAllByUserId(UUID id, Sort sort);

    OrderItemEntity findById(UUID id);

    @Query("SELECT COUNT(o) FROM OrderItemEntity o WHERE o.product.id = :productId AND o.script = true")
    int sumScriptByProductId(@Param("productId") UUID productId);

    @Query("SELECT SUM(o.performanceAmount) FROM OrderItemEntity o WHERE o.product.id = :productId")
    int sumPerformanceAmountByProductId(@Param("productId") UUID productId);

    List<OrderItemEntity> findAllByProductId(UUID productId);
}
