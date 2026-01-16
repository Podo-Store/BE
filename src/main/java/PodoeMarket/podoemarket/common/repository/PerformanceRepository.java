package PodoeMarket.podoemarket.common.repository;

import PodoeMarket.podoemarket.common.entity.PerformanceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PerformanceRepository extends JpaRepository<PerformanceEntity,Long> {
    PerformanceEntity findById(UUID id);
}
