package PodoeMarket.podoemarket.service;

import PodoeMarket.podoemarket.entity.type.ProductStatus;
import PodoeMarket.podoemarket.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Slf4j
@Service
public class AdminService {
    private final ProductRepository productRepo;

    public Long getCheckedCount(ProductStatus productStatus) {
        return productRepo.countAllByChecked(productStatus);
    }
}
