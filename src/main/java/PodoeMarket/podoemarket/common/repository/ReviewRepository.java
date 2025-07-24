package PodoeMarket.podoemarket.common.repository;

import PodoeMarket.podoemarket.common.entity.ProductEntity;
import PodoeMarket.podoemarket.common.entity.ReviewEntity;
import PodoeMarket.podoemarket.common.entity.UserEntity;
import PodoeMarket.podoemarket.common.entity.type.StandardType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ReviewRepository extends JpaRepository<ReviewEntity, Long> {
    ReviewEntity findById(UUID id);

    @Query("""
     SELECT r FROM ReviewEntity r
     JOIN FETCH r.product p
     JOIN FETCH r.user u
     WHERE p.id = :productId
    """)
    List<ReviewEntity> findAllByProductId(
            @Param("productId") UUID productId,
            Pageable pageable
    );

    Integer countByProductId(UUID productId);

    Integer countByProductIdAndRating(UUID productId, Integer rating);

    Integer countByProductIdAndStandardType(UUID productId, StandardType standardType);

    @Modifying
    @Query("UPDATE ReviewEntity r SET r.likeCount = r.likeCount + 1 WHERE r.id = :reviewId")
    void incrementLikeCount(@Param("reviewId") UUID reviewId);

    @Modifying
    @Query("UPDATE ReviewEntity r SET r.likeCount = r.likeCount - 1 WHERE r.id = :reviewId")
    void decrementLikeCount(@Param("reviewId") UUID reviewId);

    ReviewEntity findByProductAndUserId(ProductEntity product, UUID userId);

    Boolean existsByProductAndUserId(ProductEntity product, UUID userId);
}
