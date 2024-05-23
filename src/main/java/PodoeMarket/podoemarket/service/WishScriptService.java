package PodoeMarket.podoemarket.service;

import PodoeMarket.podoemarket.Utils.EntityToDTOConverter;
import PodoeMarket.podoemarket.dto.WishScriptDTO;
import PodoeMarket.podoemarket.entity.WishScriptEntity;
import PodoeMarket.podoemarket.entity.WishScriptLikeEntity;
import PodoeMarket.podoemarket.repository.WishScriptLikeRepository;
import PodoeMarket.podoemarket.repository.WishScriptRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Slf4j
@Service
public class WishScriptService {
    private final WishScriptRepository wishScriptRepo;
    private final WishScriptLikeRepository wishScriptLikeRepo;

    public void scriptCreate(final WishScriptEntity wishScriptEntity) {
        final String content = wishScriptEntity.getContent();

        if(wishScriptEntity == null) {
            throw new RuntimeException("Invalid arguments");
        }

        if(content == null || content.isBlank()) {
            throw new RuntimeException("content is invalid arguments");
        }

        wishScriptRepo.save(wishScriptEntity);
    }

    public List<WishScriptDTO> getAllEntities(UUID userId) {
        List<WishScriptEntity> wishScripts = wishScriptRepo.findAll();

        return wishScripts.stream()
                .map(wishScript -> EntityToDTOConverter.converToWishScriptDTOWithToken(wishScript, userId, wishScriptLikeRepo))
                .collect(Collectors.toList());
    }

    public List<WishScriptDTO> getAllEntities() {
        List<WishScriptEntity> wishScripts = wishScriptRepo.findAll();

        return wishScripts.stream()
                .map(wishScript -> EntityToDTOConverter.converToWishScriptDTO(wishScript, wishScriptLikeRepo))
                .collect(Collectors.toList());
    }

    public Boolean isLike(UUID userId, UUID wishScriptId) {
        return wishScriptLikeRepo.existsByUserIdAndWishScriptId(userId, wishScriptId);
    }

    public void delete(UUID userId, UUID wishScriptId) {
        WishScriptLikeEntity deleteInfo = wishScriptLikeRepo.findByUserIdAndWishScriptId(userId, wishScriptId);

        wishScriptLikeRepo.deleteById(deleteInfo.getId());
    }

    public void likeCreate(final WishScriptLikeEntity wishScriptLikeEntity) {
        wishScriptLikeRepo.save(wishScriptLikeEntity);
    }

    public WishScriptEntity script(UUID wishScriptId) {
        return wishScriptRepo.findById(wishScriptId);
    }
}
