package PodoeMarket.podoemarket.service;

import PodoeMarket.podoemarket.entity.WishScriptEntity;
import PodoeMarket.podoemarket.repository.WishScriptRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Slf4j
@Service
public class WishScriptService {
    private final WishScriptRepository wishScriptRepo;

    public WishScriptEntity create(final WishScriptEntity wishScriptEntity) {
        final String content = wishScriptEntity.getContent();

        if(wishScriptEntity == null) {
            throw new RuntimeException("Invalid arguments");
        }

        if(content == null || content.isBlank()) {
            throw new RuntimeException("content is invalid arguments");
        }

        return wishScriptRepo.save(wishScriptEntity);
    }

    public List<WishScriptEntity> getAllEntities() {
        return wishScriptRepo.findAll();
    }
}
