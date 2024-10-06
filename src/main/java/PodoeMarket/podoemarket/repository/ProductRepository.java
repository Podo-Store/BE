package PodoeMarket.podoemarket.repository;

import PodoeMarket.podoemarket.entity.ProductEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<ProductEntity, Long> {
    List<ProductEntity> findAllByUserId(UUID id, Sort sort);

    List<ProductEntity> findAllByUserId(UUID id);

    ProductEntity findById(UUID id);

    List<ProductEntity> findAllByPlayTypeAndChecked(int playType, boolean checked, Pageable pageable); // checked는 true로 고정
}
