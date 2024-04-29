package PodoeMarket.podoemarket.service;

import PodoeMarket.podoemarket.entity.ScriptEntity;
import PodoeMarket.podoemarket.repository.FileRepository;
import lombok.RequiredArgsConstructor;
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

    public String register(MultipartFile file) throws IOException {
        String filePath = file.getOriginalFilename();
        log.info("upload file: {}", filePath);

        fileRepo.save(
                ScriptEntity.builder()
                        .title(file.getOriginalFilename())
                        .type(file.getContentType())
                        .filePath(filePath)
                        .build()
        );

        // 파일 경로
        file.transferTo(new File(Objects.requireNonNull(filePath)));

        return "file uploaded successfully! filePath : " + filePath;

    }
}
