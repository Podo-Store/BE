package PodoeMarket.podoemarket.service;

import PodoeMarket.podoemarket.entity.FileEntity;
import PodoeMarket.podoemarket.entity.ProductEntity;
import PodoeMarket.podoemarket.repository.FileRepository;
import PodoeMarket.podoemarket.repository.ProductRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

@RequiredArgsConstructor
@Slf4j
@Service
public class ProductService {
    private final FileRepository fileRepo;

    public String uploadImageToFileSystem(MultipartFile file) throws IOException {
        log.info("upload file: {}", file.getOriginalFilename());
        String filePath = file.getOriginalFilename();

        FileEntity fileData = fileRepo.save(
                FileEntity.builder()
                        .name(file.getOriginalFilename())
                        .type(file.getContentType())
                        .filePath(filePath)
                        .build()
        );

        // 파일 결로
        file.transferTo(new File(Objects.requireNonNull(filePath)));

        if (fileData != null) {
            return "file uploaded successfully! filePath : " + filePath;
        }

        return null;
    }
}
