package PodoeMarket.podoemarket.repository;

import PodoeMarket.podoemarket.entity.ProductEntity;
import PodoeMarket.podoemarket.entity.type.PlayType;
import PodoeMarket.podoemarket.entity.type.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<ProductEntity, Long> {
    List<ProductEntity> findAllByUserId(UUID id, Sort sort);

    List<ProductEntity> findAllByUserId(UUID id);

    ProductEntity findById(UUID id);

    List<ProductEntity> findAllByPlayTypeAndChecked(PlayType playType, ProductStatus checked, Pageable pageable);

    Long countAllByChecked(ProductStatus checked);

    Page<ProductEntity> findByTitleContainingOrWriterContaining(String title, String writer, Pageable pageable);
}
