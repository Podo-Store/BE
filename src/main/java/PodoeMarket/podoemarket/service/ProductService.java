package PodoeMarket.podoemarket.service;

import PodoeMarket.podoemarket.entity.FileEntity;
import PodoeMarket.podoemarket.entity.ProductEntity;
import PodoeMarket.podoemarket.repository.FileRepository;
import PodoeMarket.podoemarket.repository.ProductRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Slf4j
@Service
public class ProductService {
    private final ProductRepository repo;
    private final FileRepository fileRepo;

    public ProductEntity create(final ProductEntity productEntity) {
        try {
            return repo.save(productEntity);
        } catch (Exception e) {
            log.error("ProductService.path 저장 중 예외 발생", e);
            return null;
        }
    }

    @Transactional
    public FileEntity uploadFile(String originalFileName, String filePath) {
        FileEntity fileEntity = new FileEntity();
        fileEntity.setOriginalFileName(originalFileName);
        fileEntity.setFilePath(filePath);
        // 기타 파일 정보 설정 (예: 업로드 시간 등)
        
        log.info("service 입장");

        return fileRepo.save(fileEntity);
    }
}
