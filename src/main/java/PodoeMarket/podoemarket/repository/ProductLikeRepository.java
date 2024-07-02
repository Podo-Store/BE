package PodoeMarket.podoemarket.repository;

import PodoeMarket.podoemarket.entity.ProductLikeEntity;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigInteger;
import java.util.UUID;

public interface ProductLikeRepository extends JpaRepository<ProductLikeEntity, Long> {
    BigInteger countByProductId(UUID id);

    @Query("SELECT wsl FROM ProductLikeEntity wsl WHERE wsl.user.id = :userId AND wsl.product.id = :productId")
    ProductLikeEntity findByUserIdAndProductId(UUID userId, UUID productId);

//    @Transactional
    void deleteById(UUID id);
}
