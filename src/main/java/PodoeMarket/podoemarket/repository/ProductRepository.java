package PodoeMarket.podoemarket.repository;

import PodoeMarket.podoemarket.entity.ProductEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<ProductEntity, Long> {
    List<ProductEntity> findAllByUserId(UUID id);
    ProductEntity findById(UUID id);

//    @Query("SELECT CASE WHEN COUNT(wsl) > 0 THEN true ELSE false END FROM ProductLikeEntity wsl WHERE wsl.user.id = :userId AND wsl.product.id = :productId")
//    boolean existsByUserIdAndProductId(UUID userId, UUID productId);
}
