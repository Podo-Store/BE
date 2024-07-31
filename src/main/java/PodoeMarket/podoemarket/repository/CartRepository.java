package PodoeMarket.podoemarket.repository;

import PodoeMarket.podoemarket.entity.CartEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface CartRepository extends JpaRepository<CartEntity, Long> {
    @Query("SELECT CASE WHEN COUNT(wsl) > 0 THEN true ELSE false END FROM CartEntity wsl WHERE wsl.user.id = :userId AND wsl.product.id = :productId")
    boolean existsByUserIdAndProductId(UUID userId, UUID productId);

    @Query("SELECT wsl FROM CartEntity wsl WHERE wsl.user.id = :userId AND wsl.product.id = :productId")
    CartEntity findByUserIdAndProductId(UUID userId, UUID productId);

    void deleteById(UUID id);

    List<CartEntity> findAllByUserId(UUID id);
}
