package PodoeMarket.podoemarket.repository;

import PodoeMarket.podoemarket.entity.WishScriptLikeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.UUID;

public interface WishScriptLikeRepository extends JpaRepository<WishScriptLikeEntity, UUID> {
    @Query("SELECT CASE WHEN COUNT(wsl) > 0 THEN true ELSE false END FROM WishScriptLikeEntity wsl WHERE wsl.user.id = :userId AND wsl.wish_script.id = :wishScriptId")
    boolean existsByUserIdAndWishScriptId(UUID userId, UUID wishScriptId);

    @Query("SELECT COUNT(wsl) FROM WishScriptLikeEntity wsl WHERE wsl.wish_script.id = :wishScriptId")
    int countByWishScriptId(UUID wishScriptId);

    @Query("SELECT wsl FROM WishScriptLikeEntity wsl WHERE wsl.user.id = :userId AND wsl.wish_script.id = :wishScriptId")
    WishScriptLikeEntity findByUserIdAndWishScriptId(UUID userId, UUID wishScriptId);
}
