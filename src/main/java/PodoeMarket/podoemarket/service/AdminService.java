package PodoeMarket.podoemarket.service;

import PodoeMarket.podoemarket.dto.response.ProductManagementDTO;
import PodoeMarket.podoemarket.entity.ProductEntity;
import PodoeMarket.podoemarket.entity.type.ProductStatus;
import PodoeMarket.podoemarket.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Slf4j
@Service
public class AdminService {
    private final ProductRepository productRepo;

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


    public List<ProductManagementDTO.ProductDTO> getProductList(Page<ProductEntity> productsPage) {
        return productsPage.getContent().stream()
                .map(product -> ProductManagementDTO.ProductDTO.builder()
                        .id(product.getId())
                        .createdAt(product.getCreatedAt())
                        .title(product.getTitle())
                        .writer(product.getWriter())
                        .checked(product.getChecked())
                        .playType(product.getPlayType())
                        .build())
                .collect(Collectors.toList());
    }
}
