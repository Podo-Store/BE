package PodoeMarket.podoemarket.common.repository;

import PodoeMarket.podoemarket.common.entity.ReviewEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepository extends JpaRepository<ReviewEntity, Long> {
}
