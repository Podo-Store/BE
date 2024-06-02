package PodoeMarket.podoemarket.Utils;

import PodoeMarket.podoemarket.dto.ProductDTO;
import PodoeMarket.podoemarket.dto.WishScriptDTO;
import PodoeMarket.podoemarket.entity.ProductEntity;
import PodoeMarket.podoemarket.entity.WishScriptEntity;
import PodoeMarket.podoemarket.repository.ProductLikeRepository;
import PodoeMarket.podoemarket.repository.WishScriptLikeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

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

    public static ProductDTO converToProductDTO(ProductEntity entity, ProductLikeRepository repo) {
        ProductDTO productDTO = new ProductDTO();

        productDTO.setId(entity.getId());
        productDTO.setTitle(entity.getTitle());
        // 작품 이미지 삽입 필요
        productDTO.setScript(entity.isScript());
        productDTO.setScriptPrice(entity.getScriptPrice());
        productDTO.setPerformance(entity.isPerformance());
        productDTO.setPerformancePrice(entity.getPerformancePrice());
        productDTO.setDate(entity.getDate());
        productDTO.setChecked(entity.isChecked());

         productDTO.setLikeCount(repo.countById(entity.getId())); // 좋아요 개수
        // 순위 추가

        return productDTO;
    }
}
