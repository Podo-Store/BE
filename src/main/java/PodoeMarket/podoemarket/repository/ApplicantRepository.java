package PodoeMarket.podoemarket.repository;

import PodoeMarket.podoemarket.entity.ApplicantEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ApplicantRepository extends JpaRepository<ApplicantEntity, Long> {
    ApplicantEntity findByOrderItemId(UUID orderItem);
    Boolean existsByOrderItemId(UUID orderItem);
}
