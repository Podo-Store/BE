package PodoeMarket.podoemarket.common.repository;

import PodoeMarket.podoemarket.common.entity.PerformanceEntity;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.UUID;

@Repository
public interface PerformanceRepository extends JpaRepository<PerformanceEntity,Long> {
    PerformanceEntity findById(UUID id);

    @Query("""
SELECT p FROM PerformanceEntity p
WHERE p.startDate <= :today
  AND p.endDate >= :today
  AND (:isUsed IS NULL OR p.isUsed = :isUsed)
ORDER BY p.startDate DESC
""")
    Page<PerformanceEntity> findOngoing(
            @Param("today") LocalDate today,
            @Param("isUsed") Boolean isUsed,
            Pageable pageable
    );

    @Query("""
SELECT p FROM PerformanceEntity p
WHERE p.startDate > :today
  AND (:isUsed IS NULL OR p.isUsed = :isUsed)
ORDER BY p.startDate DESC
""")
    Page<PerformanceEntity> findUpcoming(
            @Param("today") LocalDate today,
            @Param("isUsed") Boolean isUsed,
            Pageable pageable
    );

    @Query("""
SELECT p FROM PerformanceEntity p
WHERE p.endDate < :today
  AND (:isUsed IS NULL OR p.isUsed = :isUsed)
ORDER BY p.startDate DESC
""")
    Page<PerformanceEntity> findPast(
            @Param("today") LocalDate today,
            @Param("isUsed") Boolean isUsed,
            Pageable pageable
    );
}
