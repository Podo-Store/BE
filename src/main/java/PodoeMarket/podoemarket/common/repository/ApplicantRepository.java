package PodoeMarket.podoemarket.common.repository;

import PodoeMarket.podoemarket.common.entity.ApplicantEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ApplicantRepository extends JpaRepository<ApplicantEntity, Long> {
    ApplicantEntity findByOrderItemId(UUID orderItem);
    Boolean existsByOrderItemId(UUID orderItem);
}
