package PodoeMarket.podoemarket.service;

import PodoeMarket.podoemarket.Utils.EntityToDTOConverter;
import PodoeMarket.podoemarket.dto.ProductDTO;
import PodoeMarket.podoemarket.dto.ProductListDTO;
import PodoeMarket.podoemarket.entity.*;
import PodoeMarket.podoemarket.repository.BasketRepository;
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
    private final BasketRepository basketRepo;

    public List<ProductListDTO> longPlayList() {
        List<ProductEntity> longPlays = productRepo.findAllByPlayTypeAndChecked(1, true);

        return longPlays.stream()
                .map(EntityToDTOConverter::converToProductList)
                .collect(Collectors.toList());
    }

    public List<ProductListDTO> shortPlayList() {
        List<ProductEntity> shortPlays = productRepo.findAllByPlayTypeAndChecked(2, true);

        return shortPlays.stream()
                .map(EntityToDTOConverter::converToProductList)
                .collect(Collectors.toList());
    }

    public ProductEntity product(UUID id) {
        return productRepo.findById(id);
    }

    public ProductDTO productDetail(UUID productId, UUID userId) {
        ProductEntity script = product(productId);

        return EntityToDTOConverter.converToSingleProductDTO(script, userId);
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
}
