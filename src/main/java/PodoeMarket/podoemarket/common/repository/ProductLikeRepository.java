package PodoeMarket.podoemarket.common.repository;

import PodoeMarket.podoemarket.common.entity.ProductEntity;
import PodoeMarket.podoemarket.common.entity.ProductLikeEntity;
import PodoeMarket.podoemarket.common.entity.UserEntity;
import PodoeMarket.podoemarket.common.entity.type.PlayType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProductLikeRepository extends JpaRepository<ProductLikeEntity, Long> {
    Boolean existsByUserAndProductId(UserEntity user, UUID productId);

    Boolean existsByUserAndProduct(UserEntity user, ProductEntity product);

    ProductLikeEntity findByUserAndProductId(UserEntity user, UUID productId);

    List<ProductLikeEntity> findAllByUserAndProduct_PlayType(UserEntity user, PlayType playType, Pageable pageable);
}
