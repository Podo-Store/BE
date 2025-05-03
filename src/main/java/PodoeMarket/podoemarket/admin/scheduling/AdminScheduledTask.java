package PodoeMarket.podoemarket.admin.scheduling;

import PodoeMarket.podoemarket.common.entity.ProductEntity;
import PodoeMarket.podoemarket.common.entity.type.ProductStatus;
import PodoeMarket.podoemarket.common.repository.ProductRepository;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CopyObjectRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminScheduledTask {
    private final ProductRepository productRepo;
    private final AmazonS3 amazonS3;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void updateTeamStatus() {
        LocalDateTime today = LocalDate.now(ZoneId.of("Asia/Seoul")).atStartOfDay();

        // checked가 REJECT, updatedAt이 7일 이후 일 때
        List<ProductEntity> rejectProducts = productRepo.findAllByCheckedAndUpdatedAt(ProductStatus.REJECT, today.plusDays(7));

        rejectProducts.forEach(product -> {
            final String filePath = product.getFilePath().replace("script", "delete");
            moveFile(bucket, product.getFilePath(), filePath);
            deleteFile(bucket, product.getFilePath());
            product.setFilePath(filePath);
        });

        productRepo.saveAll(rejectProducts);

        log.info("작품 자동 삭제 작업이 완료되었습니다.");
    }

    // ============ private method ================
    private void moveFile(final String bucket, final String sourceKey, final String destinationKey) {
        final CopyObjectRequest copyFile = new CopyObjectRequest(bucket,sourceKey, bucket, destinationKey);

        if(amazonS3.doesObjectExist(bucket, sourceKey))
            amazonS3.copyObject(copyFile);
    }

    private void deleteFile(final String bucket, final String sourceKey) {
        if(amazonS3.doesObjectExist(bucket, sourceKey))
            amazonS3.deleteObject(bucket, sourceKey);
    }
}
