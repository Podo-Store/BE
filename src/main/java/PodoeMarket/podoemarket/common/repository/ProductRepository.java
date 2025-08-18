package PodoeMarket.podoemarket.common.repository;

import PodoeMarket.podoemarket.common.entity.ProductEntity;
import PodoeMarket.podoemarket.common.entity.type.PlayType;
import PodoeMarket.podoemarket.common.entity.type.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<ProductEntity, Long> {
    List<ProductEntity> findAllByUserId(UUID id);

    ProductEntity findById(UUID id);

    @Query("SELECT p FROM ProductEntity p " +
            "WHERE p.playType = :playType " +
            "AND p.checked = :checked " +
            "AND p.isDelete = false " +
            "AND (p.script = true OR p.performance = true)")
    List<ProductEntity> findAllValidPlays(
            @Param("playType") PlayType playType,
            @Param("checked") ProductStatus checked,
            Pageable pageable
    );

    Long countAllByChecked(ProductStatus checked);

    Page<ProductEntity> findByTitleContainingOrWriterContaining(String title, String writer, Pageable pageable);

    Page<ProductEntity> findByChecked(ProductStatus productStatus, Pageable pageable);

    Page<ProductEntity> findByTitleContainingOrWriterContainingAndChecked(String title, String writer, ProductStatus checked, Pageable pageable);

    List<ProductEntity> findAllByCheckedAndUpdatedAt(ProductStatus checked, LocalDateTime updatedAt);

    List<ProductEntity> findAllByIsDeleteAndUpdatedAt(Boolean isDelete, LocalDateTime updatedAt);

    @Modifying
    @Query("UPDATE ProductEntity p SET p.likeCount = p.likeCount + 1 WHERE p.id = :productId")
    void incrementLikeCount(@Param("productId") UUID productId);

    @Modifying
    @Query("UPDATE ProductEntity p SET p.likeCount = p.likeCount - 1 WHERE p.id = :productId")
    void decrementLikeCount(@Param("productId") UUID productId);

    @Modifying
    @Query("UPDATE ProductEntity p SET p.viewCount = COALESCE(p.viewCount,0) + :delta WHERE p.id = :id")
    void incrementViewCount(@Param("id") UUID id, @Param("delta") long delta);

    @Query("SELECT SUM(p.viewCount) FROM ProductEntity p")
    long sumViewCount();
}
