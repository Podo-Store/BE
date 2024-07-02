package PodoeMarket.podoemarket.service;

import PodoeMarket.podoemarket.entity.ProductEntity;
import PodoeMarket.podoemarket.entity.ProductLikeEntity;
import PodoeMarket.podoemarket.repository.ProductLikeRepository;
import PodoeMarket.podoemarket.repository.ProductRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@RequiredArgsConstructor
@Slf4j
@Service
public class ProductService {
    private final ProductRepository productRepo;
    private final ProductLikeRepository productLikeRepo;

    public ProductEntity product(UUID id) {
        return productRepo.findById(id);
    }

    public Boolean isLike(UUID userId, UUID productId) {
        return productRepo.existsByUserIdAndProductId(userId, productId);
    }

    @Transactional
    public void delete(UUID userId, UUID productId) {
        ProductLikeEntity deleteInfo = productLikeRepo.findByUserIdAndProductId(userId,productId);

        productLikeRepo.deleteById(deleteInfo.getId());
    }

    public void likeCreate(final ProductLikeEntity productLikeEntity) {
        productLikeRepo.save(productLikeEntity);
    }
}
