package PodoeMarket.podoemarket.repository;

import PodoeMarket.podoemarket.entity.BasketEntity;
import PodoeMarket.podoemarket.entity.ProductLikeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.UUID;

public interface BasketRepository extends JpaRepository<BasketEntity, Long> {
    @Query("SELECT CASE WHEN COUNT(wsl) > 0 THEN true ELSE false END FROM BasketEntity wsl WHERE wsl.user.id = :userId AND wsl.product.id = :productId")
    boolean existsByUserIdAndProductId(UUID userId, UUID productId);

    @Query("SELECT wsl FROM BasketEntity wsl WHERE wsl.user.id = :userId AND wsl.product.id = :productId")
    BasketEntity findByUserIdAndProductId(UUID userId, UUID productId);

    void deleteById(UUID id);

}
