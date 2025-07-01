package PodoeMarket.podoemarket.common.repository;

import PodoeMarket.podoemarket.common.entity.ReviewEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
