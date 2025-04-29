package PodoeMarket.podoemarket.common.repository;

import PodoeMarket.podoemarket.common.entity.ProductLikeEntity;
import PodoeMarket.podoemarket.common.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ProductLikeRepository extends JpaRepository<ProductLikeEntity, Long> {
    Boolean existsByUserAndProductId(UserEntity user, UUID productId);

    ProductLikeEntity findByUserAndProductId(UserEntity user, UUID productId);

    int countByProductId(UUID productId);
}
