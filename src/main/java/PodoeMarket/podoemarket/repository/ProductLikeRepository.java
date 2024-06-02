package PodoeMarket.podoemarket.repository;

import PodoeMarket.podoemarket.entity.ProductLikeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigInteger;
import java.util.UUID;

public interface ProductLikeRepository extends JpaRepository<ProductLikeEntity, Long> {
    BigInteger countById(UUID id);
}
