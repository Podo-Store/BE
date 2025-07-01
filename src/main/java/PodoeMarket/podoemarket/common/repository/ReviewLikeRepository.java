package PodoeMarket.podoemarket.common.repository;

import PodoeMarket.podoemarket.common.entity.ReviewLikeEntity;
import PodoeMarket.podoemarket.common.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ReviewLikeRepository extends JpaRepository<ReviewLikeEntity, Long> {
    Boolean existsByUserAndReviewId(UserEntity user, UUID reviewId);
}
