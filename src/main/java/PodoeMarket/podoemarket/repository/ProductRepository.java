package PodoeMarket.podoemarket.repository;

import PodoeMarket.podoemarket.entity.FileEntity;
import PodoeMarket.podoemarket.entity.ProductEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<FileEntity, Long> {
    Optional<FileEntity> findByName(String fileName);
}
