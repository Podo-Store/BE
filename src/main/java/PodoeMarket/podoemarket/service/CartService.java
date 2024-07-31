package PodoeMarket.podoemarket.service;

import PodoeMarket.podoemarket.Utils.EntityToDTOConverter;
import PodoeMarket.podoemarket.dto.ProductListDTO;
import PodoeMarket.podoemarket.entity.CartEntity;
import PodoeMarket.podoemarket.repository.CartRepository;
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
public class CartService {
    private final CartRepository cartRepo;

    public CartEntity product(UUID userId, UUID productId) {
        return cartRepo.findByUserIdAndProductId(userId, productId);
    }

    public Boolean isCart(UUID userId, UUID productId) {
        return cartRepo.existsByUserIdAndProductId(userId, productId);
    }

    @Transactional
    public void cartDelete(UUID userId, UUID productId) {
        CartEntity deleteInfo = cartRepo.findByUserIdAndProductId(userId, productId);

        cartRepo.deleteById(deleteInfo.getId());
    }

    public void cartCreate(final CartEntity CartEntity) {
        cartRepo.save(CartEntity);
    }

    public List<ProductListDTO> getAllCartProducts(UUID id) {
        List<CartEntity> products = cartRepo.findAllByUserId(id);

        return products.stream()
                .map(EntityToDTOConverter::convertToBasketList)
                .collect(Collectors.toList());
    }
}
