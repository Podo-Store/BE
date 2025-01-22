package PodoeMarket.podoemarket.admin.service;

import PodoeMarket.podoemarket.admin.dto.response.ProductManagementResponseDTO;
import PodoeMarket.podoemarket.entity.ProductEntity;
import PodoeMarket.podoemarket.entity.UserEntity;
import PodoeMarket.podoemarket.entity.type.ProductStatus;
import PodoeMarket.podoemarket.repository.ProductRepository;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.S3Object;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.Days;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Slf4j
@Service
public class AdminService {
    private final ProductRepository productRepo;
    private final AmazonS3 amazonS3;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    public void checkAuth(final UserEntity user) {
        if (!user.isAuth())
            throw new RuntimeException("어드민이 아닙니다.");
    }

    public Long getCheckedCount(final ProductStatus productStatus) {
        return productRepo.countAllByChecked(productStatus);
    }

    public Page<ProductEntity> getAllProducts(final String search, final ProductStatus status, final int page) {
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

    public ProductEntity getProduct(final UUID id) {
        return productRepo.findById(id);
    }

    public void updateProduct(final ProductEntity product) {
        productRepo.save(product);

        // 거절 설정 7일 이후 자동 삭제

    }

    public void checkExpire(final LocalDateTime updatedAt, final ProductStatus productStatus) {
        if (productStatus == ProductStatus.REJECT && updatedAt.isAfter(LocalDateTime.now().plusDays(7))) {
            throw new RuntimeException("등록 거절 이후 7일이 지났습니다.");
        }
    }

    public byte[] downloadFile(final String fileKey) {
        try (S3Object s3Object = amazonS3.getObject(bucket, fileKey);
             InputStream inputStream = s3Object.getObjectContent();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            // 버퍼를 사용하여 데이터 읽기
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            return outputStream.toByteArray();
        } catch (AmazonS3Exception e) {
            throw new RuntimeException("S3에서 파일을 찾을 수 없습니다: " + fileKey);
        } catch (IOException e) {
            throw new RuntimeException("파일 다운로드 중 오류가 발생했습니다: " + fileKey);
        }
    }

    // =========== private method ============

}
