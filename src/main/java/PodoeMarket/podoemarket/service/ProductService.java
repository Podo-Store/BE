package PodoeMarket.podoemarket.service;

import PodoeMarket.podoemarket.entity.ProductEntity;
import PodoeMarket.podoemarket.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Objects;

@RequiredArgsConstructor
@Slf4j
@Service
public class ProductService {
    private final ProductRepository fileRepo;

    public void register(ProductEntity scriptEntity) {
        if (!Objects.equals(scriptEntity.getFileType(), "application/pdf")) {
            throw new RuntimeException("contentType is not PDF");
        }

        fileRepo.save(scriptEntity);
    }
}
