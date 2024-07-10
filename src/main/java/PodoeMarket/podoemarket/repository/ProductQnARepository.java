package PodoeMarket.podoemarket.repository;

import PodoeMarket.podoemarket.entity.ProductQnAEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProductQnARepository extends JpaRepository<ProductQnAEntity, Long> {
    List<ProductQnAEntity> findAllByProductId(UUID id);
}
