package PodoeMarket.podoemarket.service;

import PodoeMarket.podoemarket.entity.ProductEntity;
import PodoeMarket.podoemarket.repository.ProductRepository;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;

@RequiredArgsConstructor
@Slf4j
@Service
public class RegisterService {
    private final ProductRepository fileRepo;
    private final AmazonS3 amazonS3;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    @Value("${cloud.aws.s3.folder.folderName1}")
    private String bucketFolder;

    public void register(ProductEntity scriptEntity) {
        fileRepo.save(scriptEntity);
    }

    public String uploadScript(MultipartFile[] files) throws IOException {
        if(files[0].isEmpty()) {
            throw new RuntimeException("선택된 파일이 없음");
        }
        else if(files.length > 1) {
            throw new RuntimeException("파일 수가 1개를 초과함");
        }

        if (!Objects.equals(files[0].getContentType(), "application/pdf")) {
            throw new RuntimeException("contentType is not PDF");
        }

        // 파일 이름 가공
        SimpleDateFormat dateFormat = new SimpleDateFormat( "yyyyMMddHHmmss");
        Date time = new Date();
        String name = files[0].getOriginalFilename();
        String[] fileName = new String[]{Objects.requireNonNull(name).substring(0, name.length() - 4)};

        String filePath = bucketFolder + fileName[0] + dateFormat.format(time);

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(files[0].getSize());
        metadata.setContentType(files[0].getContentType());

        amazonS3.putObject(bucket, filePath, files[0].getInputStream(), metadata);

        return amazonS3.getUrl(bucket, filePath).toString();
    }

}
