package PodoeMarket.podoemarket.service;

import PodoeMarket.podoemarket.entity.ProductEntity;
import PodoeMarket.podoemarket.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Slf4j
@Service
public class ProductService {
    private final ProductRepository repo;

    public ProductEntity create(final ProductEntity productEntity) {
        return repo.save(productEntity);
    }
}
