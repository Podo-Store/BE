package PodoeMarket.podoemarket.Utils;

import PodoeMarket.podoemarket.dto.ProductDTO;
import PodoeMarket.podoemarket.dto.ProductListDTO;
import PodoeMarket.podoemarket.dto.WishScriptDTO;
import PodoeMarket.podoemarket.entity.ProductEntity;
import PodoeMarket.podoemarket.entity.ProductLikeEntity;
import PodoeMarket.podoemarket.entity.WishScriptEntity;
import PodoeMarket.podoemarket.repository.ProductLikeRepository;
import PodoeMarket.podoemarket.repository.WishScriptLikeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class EntityToDTOConverter {
    public static WishScriptDTO converToWishScriptDTO(WishScriptEntity entity, WishScriptLikeRepository repo){
        WishScriptDTO wishScriptDTO = new WishScriptDTO();

        wishScriptDTO.setId(entity.getId());
        wishScriptDTO.setContent(entity.getContent());
        wishScriptDTO.setGenre(entity.getGenre());
        wishScriptDTO.setCharacterNumber(entity.getCharacterNumber());
        wishScriptDTO.setRuntime(entity.getRuntime());
        wishScriptDTO.setDate(entity.getDate());

        wishScriptDTO.setNickname(entity.getUser().getNickname());
        wishScriptDTO.setProfileFilePath(entity.getUser().getFilePath());
        wishScriptDTO.setLike(false);
        wishScriptDTO.setLikeCount(repo.countByWishScriptId(entity.getId()));

        return wishScriptDTO;
    }

    public static WishScriptDTO converToWishScriptDTOWithToken(WishScriptEntity entity, UUID id, WishScriptLikeRepository repo){
        WishScriptDTO wishScriptDTO = new WishScriptDTO();

        wishScriptDTO.setId(entity.getId());
        wishScriptDTO.setContent(entity.getContent());
        wishScriptDTO.setGenre(entity.getGenre());
        wishScriptDTO.setCharacterNumber(entity.getCharacterNumber());
        wishScriptDTO.setRuntime(entity.getRuntime());
        wishScriptDTO.setDate(entity.getDate());

        if (entity.getUser() != null) {
            wishScriptDTO.setNickname(entity.getUser().getNickname());
            wishScriptDTO.setProfileFilePath(entity.getUser().getFilePath());
            wishScriptDTO.setLike(repo.existsByUserIdAndWishScriptId(id, entity.getId()));
        }
        wishScriptDTO.setLikeCount(repo.countByWishScriptId(entity.getId()));

        return wishScriptDTO;
    }

    public static ProductListDTO converToProductList(ProductEntity entity, ProductLikeRepository repo) {
        ProductListDTO productListDTO = new ProductListDTO();

        productListDTO.setId(entity.getId());
        productListDTO.setTitle(entity.getTitle());
        productListDTO.setWriter(entity.getWriter());
        productListDTO.setImagePath(entity.getImagePath());
        productListDTO.setScript(entity.isScript());
        productListDTO.setScriptPrice(entity.getScriptPrice());
        productListDTO.setPerformance(entity.isPerformance());
        productListDTO.setPerformancePrice(entity.getPerformancePrice());
        productListDTO.setDate(entity.getDate());
        productListDTO.setChecked(entity.isChecked());

        productListDTO.setLikeCount(repo.countByProductId(entity.getId())); // 좋아요 개수
        // 순위 추가

        return productListDTO;
    }

    public static ProductDTO converToSingleProductDTO(ProductEntity entity, ProductLikeRepository repo, UUID userId) {
        ProductDTO productDTO = new ProductDTO();

        productDTO.setId(entity.getId());
        productDTO.setTitle(entity.getTitle());
        productDTO.setWriter(entity.getWriter());
        productDTO.setFilePath(entity.getFilePath());
        productDTO.setImagePath(entity.getImagePath());
        productDTO.setContent(entity.getStory());
        productDTO.setScript(entity.isScript());
        productDTO.setScriptPrice(entity.getScriptPrice());
        productDTO.setPerformance(entity.isPerformance());
        productDTO.setPerformancePrice(entity.getPerformancePrice());
        productDTO.setContent(entity.getContent());
        productDTO.setDate(entity.getDate());
        productDTO.setStory(entity.getStory());
        productDTO.setGenre(entity.getGenre());
        productDTO.setCharacterNumber(entity.getCharacterNumber());
        productDTO.setRuntime(entity.getRuntime());
        productDTO.setChecked(entity.isChecked());

//        productDTO.setLikeCount(repo.countByProductId(entity.getId())); // 좋아요 개수
        productDTO.setLike(repo.existsByUserIdAndProductId(userId, entity.getId()));

        return productDTO;
    }

    public static ProductListDTO converToProductLikeList(ProductLikeEntity entity, ProductLikeRepository repo, UUID id) {
        ProductListDTO productListDTO = new ProductListDTO();

        productListDTO.setId(entity.getProduct().getId());
        productListDTO.setTitle(entity.getProduct().getTitle());
        productListDTO.setWriter(entity.getProduct().getWriter());
        productListDTO.setImagePath(entity.getProduct().getImagePath());
        productListDTO.setScript(entity.getProduct().isScript());
        productListDTO.setScriptPrice(entity.getProduct().getScriptPrice());
        productListDTO.setPerformance(entity.getProduct().isPerformance());
        productListDTO.setPerformancePrice(entity.getProduct().getPerformancePrice());
        productListDTO.setDate(entity.getProduct().getDate());
        productListDTO.setChecked(entity.getProduct().isChecked());

        productListDTO.setLikeCount(repo.countByProductId(entity.getProduct().getId())); // 좋아요 개수
        productListDTO.setLike(repo.existsByUserIdAndProductId(id, entity.getProduct().getId()));

        return productListDTO;
    }
}
