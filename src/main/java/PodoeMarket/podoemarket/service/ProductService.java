package PodoeMarket.podoemarket.service;

import PodoeMarket.podoemarket.Utils.EntityToDTOConverter;
import PodoeMarket.podoemarket.dto.ProductDTO;
import PodoeMarket.podoemarket.dto.ProductListDTO;
import PodoeMarket.podoemarket.entity.*;
import PodoeMarket.podoemarket.repository.ProductRepository;
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

    public List<ProductListDTO> longPlayList() {
        List<ProductEntity> longPlays = productRepo.findAllByPlayTypeAndChecked(1, true);

        return longPlays.stream()
                .map(EntityToDTOConverter::convertToProductList)
                .collect(Collectors.toList());
    }

    public List<ProductListDTO> shortPlayList() {
        List<ProductEntity> shortPlays = productRepo.findAllByPlayTypeAndChecked(2, true);

        return shortPlays.stream()
                .map(EntityToDTOConverter::convertToProductList)
                .collect(Collectors.toList());
    }

    public ProductEntity product(UUID id) {
        return productRepo.findById(id);
    }

    public ProductDTO productDetail(UUID productId, UUID userId) {
        ProductEntity script = product(productId);

        return EntityToDTOConverter.convertToSingleProductDTO(script, userId);
    }
}
