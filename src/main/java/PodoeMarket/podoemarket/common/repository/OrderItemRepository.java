package PodoeMarket.podoemarket.common.repository;

import PodoeMarket.podoemarket.common.entity.OrderItemEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface OrderItemRepository extends JpaRepository<OrderItemEntity, Long> {
    List<OrderItemEntity> findByProductIdAndUserId(UUID productId, UUID userId);

    List<OrderItemEntity> findByOrderId(Long id);

    List<OrderItemEntity> findAllByUserIdAndScript(UUID id, boolean script, Sort sort);

    List<OrderItemEntity> findAllByUserId(UUID id, Sort sort);

    OrderItemEntity findById(UUID id);

    @Query("SELECT COUNT(o) FROM OrderItemEntity o WHERE o.product.id = :productId AND o.script = true")
    int sumScriptByProductId(@Param("productId") UUID productId);

    @Query("SELECT COALESCE(SUM(o.performanceAmount), 0) FROM OrderItemEntity o WHERE o.product.id = :productId")
    int sumPerformanceAmountByProductId(@Param("productId") UUID productId);

    List<OrderItemEntity> findAllByProductId(UUID productId);

    @Query("""
    SELECT oi FROM OrderItemEntity oi
    JOIN oi.product p
    JOIN p.user u
    WHERE p.title LIKE %:keyword%
    OR p.writer LIKE %:keyword%
    OR u.nickname LIKE %:keyword%
    """)
    Page<OrderItemEntity> findOrderItemsByKeyword(@Param("keyword") String keyword,
                                                  Pageable pageable);

    Boolean existsByProductIdAndUserId(UUID productId, UUID userId);
}
