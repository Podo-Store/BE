package PodoeMarket.podoemarket.common.repository;

import PodoeMarket.podoemarket.common.entity.RefundEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface RefundRepository extends JpaRepository<RefundEntity, Long> {
    int countByOrderId(Long orderId);

    @Query("""
    SELECT r.order.id, COUNT(r)
    FROM RefundEntity r
    WHERE r.order.id IN :orderIds
    GROUP BY r.order.id
""")
    List<Object[]> countByOrderIds(@Param("orderIds") List<Long> orderIds);

    @Query("""
    SELECT COALESCE(SUM(r.quantity), 0)
    FROM RefundEntity r
    WHERE r.order.id IN (
        SELECT oi.order.id
        FROM OrderItemEntity oi
        WHERE oi.product.id = :productId
    )
""")
    long sumRefundQuantityByProductId(
            @Param("productId") UUID productId
    );

    @Query("""
    SELECT COALESCE(SUM(r.price), 0)
    FROM RefundEntity r
    WHERE r.order.id = :orderId
""")
    long sumRefundPriceByOrder(@Param("orderId") Long orderId);
}
