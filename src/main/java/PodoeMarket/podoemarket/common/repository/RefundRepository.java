package PodoeMarket.podoemarket.common.repository;

import PodoeMarket.podoemarket.common.entity.RefundEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RefundRepository extends JpaRepository<RefundEntity, Long> {
    int countByOrderId(Long orderId);

    @Query("""
    SELECT r.order.id, COUNT(r)
    FROM RefundEntity r
    WHERE r.order.id IN :orderIds
    GROUP BY r.order.id
""")
    List<Object[]> countByOrderIds(@Param("orderIds") List<Long> orderIds);
}
