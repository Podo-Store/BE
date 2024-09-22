package PodoeMarket.podoemarket.repository;

import PodoeMarket.podoemarket.entity.PerformanceDateEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PerformanceDateRepository extends JpaRepository<PerformanceDateEntity, Long> {
    int countByOrderItemId(UUID id);
}