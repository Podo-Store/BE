package PodoeMarket.podoemarket.repository;

import PodoeMarket.podoemarket.entity.ProductEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends JpaRepository<ProductEntity, Long> {
    ProductEntity findByWriter(String writer);
}
