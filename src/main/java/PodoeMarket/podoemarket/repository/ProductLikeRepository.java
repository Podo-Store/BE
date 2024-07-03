package PodoeMarket.podoemarket.repository;

import PodoeMarket.podoemarket.entity.ProductEntity;
import PodoeMarket.podoemarket.entity.ProductLikeEntity;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigInteger;
import java.util.List;
import java.util.UUID;

public interface ProductLikeRepository extends JpaRepository<ProductLikeEntity, Long> {
    BigInteger countByProductId(UUID id);

    @Query("SELECT wsl FROM ProductLikeEntity wsl WHERE wsl.user.id = :userId AND wsl.product.id = :productId")
    ProductLikeEntity findByUserIdAndProductId(UUID userId, UUID productId);

    void deleteById(UUID id);

    List<ProductLikeEntity> findAllByUserId(UUID id);

    @Query("SELECT CASE WHEN COUNT(wsl) > 0 THEN true ELSE false END FROM ProductLikeEntity wsl WHERE wsl.user.id = :userId AND wsl.product.id = :productId")
    boolean existsByUserIdAndProductId(UUID userId, UUID productId);
}
