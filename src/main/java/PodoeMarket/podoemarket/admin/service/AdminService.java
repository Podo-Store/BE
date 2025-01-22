package PodoeMarket.podoemarket.admin.service;

import PodoeMarket.podoemarket.admin.dto.response.ProductManagementResponseDTO;
import PodoeMarket.podoemarket.entity.ProductEntity;
import PodoeMarket.podoemarket.entity.UserEntity;
import PodoeMarket.podoemarket.entity.type.ProductStatus;
import PodoeMarket.podoemarket.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Slf4j
@Service
public class AdminService {
    private final ProductRepository productRepo;

    public void checkAuth(UserEntity user) {
        if (!user.isAuth())
            throw new RuntimeException("어드민이 아닙니다.");
    }

    public Long getCheckedCount(ProductStatus productStatus) {
        return productRepo.countAllByChecked(productStatus);
    }

    public Page<ProductEntity> getAllProducts(String search, ProductStatus status, int page) {
        final PageRequest pageRequest = PageRequest.of(page, 10);

        if (search == null || search.trim().isEmpty()) {
            if (status == null) // 검색어 X, 전체 O
                return productRepo.findAll(pageRequest);
            else // 검색어 X, 전체 X
                return productRepo.findByChecked(status, pageRequest);
        } else {
            if (status == null) // 검색어 O, 전체 O
                return productRepo.findByTitleContainingOrWriterContaining(search, search, pageRequest);
            else // 검색어 O, 전체 X
                return productRepo.findByTitleContainingOrWriterContainingAndChecked(search, search, status, pageRequest);
        }
    }

    public List<ProductManagementResponseDTO.ProductDTO> getProductList(Page<ProductEntity> productsPage) {
        return productsPage.getContent().stream()
                .map(product -> ProductManagementResponseDTO.ProductDTO.builder()
                        .id(product.getId())
                        .createdAt(product.getCreatedAt())
                        .title(product.getTitle())
                        .writer(product.getWriter())
                        .checked(product.getChecked())
                        .playType(product.getPlayType())
                        .build())
                .collect(Collectors.toList());
    }

    public ProductEntity getProduct(UUID id) {
        return productRepo.findById(id);
    }

    public void updateProduct(ProductEntity product) {
        productRepo.save(product);
    }
}
