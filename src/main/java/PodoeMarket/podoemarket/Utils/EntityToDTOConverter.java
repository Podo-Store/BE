package PodoeMarket.podoemarket.Utils;

import PodoeMarket.podoemarket.dto.WishScriptDTO;
import PodoeMarket.podoemarket.entity.WishScriptEntity;
import PodoeMarket.podoemarket.repository.WishScriptLikeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class EntityToDTOConverter {
    public static WishScriptDTO converToWishScriptDTO(WishScriptEntity entity, UUID id, WishScriptLikeRepository repo){
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
}
