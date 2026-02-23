package PodoeMarket.podoemarket.common.repository;

import PodoeMarket.podoemarket.common.entity.PdfDownloadLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface PdfDownloadLogRepository extends JpaRepository<PdfDownloadLogEntity, Long> {
    @Query("""
    SELECT p.orderItemId
    FROM PdfDownloadLogEntity p
    WHERE p.orderItemId IN :orderItemIds
    AND p.userId = :userId
""")
    List<UUID> findDownloadedOrderItemIds(@Param("orderItemIds") List<UUID> orderItemIds, @Param("userId") UUID userId);

    boolean existsByOrderItemIdAndUserId(UUID orderItemId, UUID userId);
}
