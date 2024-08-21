package PodoeMarket.podoemarket.service;

import PodoeMarket.podoemarket.Utils.EntityToDTOConverter;
import PodoeMarket.podoemarket.dto.ProductDTO;
import PodoeMarket.podoemarket.dto.ProductListDTO;
import PodoeMarket.podoemarket.entity.*;
import PodoeMarket.podoemarket.repository.OrderItemRepository;
import PodoeMarket.podoemarket.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Slf4j
@Service
public class ProductService {
    private final ProductRepository productRepo;
    private final OrderItemRepository orderItemRepo;

    @Value("${cloud.aws.s3.url}")
    private String bucketURL;

    public List<ProductListDTO> longPlayList() {
        final List<ProductEntity> longPlays = productRepo.findAllByPlayTypeAndChecked(1, true);

        return longPlays.stream()
                .map(entity -> EntityToDTOConverter.convertToProductList(entity, bucketURL))
                .collect(Collectors.toList());
    }

    public List<ProductListDTO> shortPlayList() {
        final List<ProductEntity> shortPlays = productRepo.findAllByPlayTypeAndChecked(2, true);

        return shortPlays.stream()
                .map(entity -> EntityToDTOConverter.convertToProductList(entity, bucketURL))
                .collect(Collectors.toList());
    }

    public ProductEntity product(UUID id) {
        return productRepo.findById(id);
    }

    public boolean isBuyScript(UUID userId, UUID productId) {
        final List<OrderItemEntity> orderitems = orderItemRepo.findByProductIdAndUserId(productId, userId);

        for(OrderItemEntity item : orderitems) {
            if(item.isScript()) {
                return true;
            }
        }
        return false;
    }

    public ProductDTO productDetail(UUID productId, boolean isBuyScript) {
        final ProductEntity script = product(productId);

        return EntityToDTOConverter.convertToSingleProductDTO(script, isBuyScript, bucketURL);
    }
}
