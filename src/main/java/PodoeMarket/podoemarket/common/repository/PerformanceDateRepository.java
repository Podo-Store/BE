package PodoeMarket.podoemarket.common.repository;

import PodoeMarket.podoemarket.common.entity.PerformanceDateEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface PerformanceDateRepository extends JpaRepository<PerformanceDateEntity, Long> {
    int countByOrderItemId(UUID id);

    @Query("""
    SELECT pd.orderItem.id, COUNT(pd)
    FROM PerformanceDateEntity pd
    WHERE pd.orderItem.id IN :orderItemIds
    GROUP BY pd.orderItem.id
""")
    List<Object[]> countByOrderItemIds(@Param("orderItemIds") List<UUID> orderItemIds);
}
