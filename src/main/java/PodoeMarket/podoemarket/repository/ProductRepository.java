package PodoeMarket.podoemarket.repository;

import PodoeMarket.podoemarket.entity.ProductEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<ProductEntity, Long> {
    List<ProductEntity> findAllByWriter(String writer);
    ProductEntity findById(UUID id);
}
