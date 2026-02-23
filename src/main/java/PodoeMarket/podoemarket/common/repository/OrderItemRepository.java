package PodoeMarket.podoemarket.common.repository;

import PodoeMarket.podoemarket.common.entity.OrderItemEntity;
import PodoeMarket.podoemarket.common.entity.type.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface OrderItemRepository extends JpaRepository<OrderItemEntity, Long> {
    List<OrderItemEntity> findByOrderId(Long id);

    @Query("""
    SELECT oi
    FROM OrderItemEntity oi
    JOIN oi.order o
    WHERE oi.user.id = :userId
    AND oi.script = true
    AND o.orderStatus = :status
    """)
    List<OrderItemEntity> findPaidScriptOrderItems(
            @Param("userId") UUID userId,
            @Param("status") OrderStatus status,
            Sort sort
    );

    @Query("""
    SELECT oi
    FROM OrderItemEntity oi
    JOIN oi.order o
    WHERE oi.user.id = :userId
    AND oi.performanceAmount > 0
    AND o.orderStatus = :status
    """)
    List<OrderItemEntity> findPaidPerformanceOrderItems(
            @Param("userId") UUID userId,
            @Param("status") OrderStatus status,
            Sort sort
    );

    OrderItemEntity findById(UUID id);

    @Query("SELECT COUNT(o) FROM OrderItemEntity o WHERE o.product.id = :productId AND o.script = true")
    int sumScriptByProductId(@Param("productId") UUID productId);

    @Query("""
    SELECT COALESCE(SUM(oi.performanceAmount), 0)
    FROM OrderItemEntity oi
    JOIN oi.order o
    WHERE oi.product.id = :productId
    AND o.orderStatus = :status
    """)
    long sumPaidPerformanceAmountByProductId(@Param("productId") UUID productId, @Param("status") OrderStatus status);

    List<OrderItemEntity> findAllByProductId(UUID productId);

    @Query("""
    SELECT oi FROM OrderItemEntity oi
    JOIN oi.product p
    JOIN p.user u
    WHERE p.title LIKE %:keyword%
    OR p.writer LIKE %:keyword%
    OR u.nickname LIKE %:keyword%
""")
    Page<OrderItemEntity> findOrderItemsByKeyword(@Param("keyword") String keyword, Pageable pageable);

    Boolean existsByProduct_IdAndUser_IdAndScriptTrueAndOrder_OrderStatusAndCreatedAtAfter(UUID productId, UUID userId, OrderStatus status, LocalDateTime oneYearAgo);

    @Query("""
    SELECT MAX(o.createdAt)
    FROM OrderItemEntity o
    WHERE o.product.id = :productId
    AND o.user.id = :userId
    AND o.script = true
    AND o.order.orderStatus = :status
    """)
    LocalDateTime findLastScriptPurchaseDate(@Param("productId") UUID productId, @Param("userId") UUID userId, @Param("status") OrderStatus status);
}
