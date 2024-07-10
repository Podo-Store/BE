package PodoeMarket.podoemarket.service;

import PodoeMarket.podoemarket.Utils.EntityToDTOConverter;
import PodoeMarket.podoemarket.dto.ProductDTO;
import PodoeMarket.podoemarket.dto.ProductListDTO;
import PodoeMarket.podoemarket.dto.ProductQnADTO;
import PodoeMarket.podoemarket.entity.*;
import PodoeMarket.podoemarket.repository.BasketRepository;
import PodoeMarket.podoemarket.repository.ProductLikeRepository;
import PodoeMarket.podoemarket.repository.ProductQnARepository;
import PodoeMarket.podoemarket.repository.ProductRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Slf4j
@Service
public class ProductService {
    private final ProductRepository productRepo;
    private final ProductLikeRepository productLikeRepo;
    private final BasketRepository basketRepo;
    private final ProductQnARepository productQnARepo;

    public ProductEntity product(UUID id) {
        return productRepo.findById(id);
    }

    public Boolean isLike(UUID userId, UUID productId) {
        return productLikeRepo.existsByUserIdAndProductId(userId, productId);
    }

    @Transactional
    public void likeDelete(UUID userId, UUID productId) {
        ProductLikeEntity deleteInfo = productLikeRepo.findByUserIdAndProductId(userId,productId);

        productLikeRepo.deleteById(deleteInfo.getId());
    }

    public void likeCreate(final ProductLikeEntity productLikeEntity) {
        productLikeRepo.save(productLikeEntity);
    }

    public ProductDTO productDetail(UUID productId, UUID userId) {
        ProductEntity script = product(productId);

        return EntityToDTOConverter.converToSingleProductDTO(script, productLikeRepo, userId);
    }

    public Boolean isBasket(UUID userId, UUID productId) {
        return basketRepo.existsByUserIdAndProductId(userId, productId);
    }

    @Transactional
    public void basketDelete(UUID userId, UUID productId) {
        BasketEntity deleteInfo = basketRepo.findByUserIdAndProductId(userId, productId);

        basketRepo.deleteById(deleteInfo.getId());
    }

    public void basketCreate(final BasketEntity BasketEntity) {
        basketRepo.save(BasketEntity);
    }

    public List<ProductListDTO> getAllBasketProducts(UUID id) {
        List<BasketEntity> products = basketRepo.findAllByUserId(id);

        return products.stream()
                .map(EntityToDTOConverter::converToBasketList)
                .collect(Collectors.toList());
    }

    public void writeQuestion(final ProductQnAEntity productQnAEntity, final UUID userId) {
        if(productQnAEntity.getUser().getId() == userId) {
            throw new RuntimeException("user is owner");
        }

        productQnARepo.save(productQnAEntity);
    }

    public List<ProductQnADTO> getProductQnA(UUID id) {
        List<ProductQnAEntity> lists = productQnARepo.findAllByProductId(id);

        return lists.stream()
                .map(EntityToDTOConverter::converToProductQnAList)
                .collect(Collectors.toList());
    }
}
