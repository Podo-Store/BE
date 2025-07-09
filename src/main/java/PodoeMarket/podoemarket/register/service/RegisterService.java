package PodoeMarket.podoemarket.register.service;

import PodoeMarket.podoemarket.common.entity.ProductEntity;
import PodoeMarket.podoemarket.common.entity.UserEntity;
import PodoeMarket.podoemarket.common.entity.type.ProductStatus;
import PodoeMarket.podoemarket.common.repository.ProductRepository;
import PodoeMarket.podoemarket.common.repository.UserRepository;
import PodoeMarket.podoemarket.service.MailSendService;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import org.apache.commons.io.FilenameUtils;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.Normalizer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@RequiredArgsConstructor
@Slf4j
@Service
@Transactional(readOnly = true)
public class RegisterService {
    private final ProductRepository fileRepo;
    private final UserRepository userRepo;
    private final AmazonS3 amazonS3;
    private final MailSendService mailSendService;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    @Value("${cloud.aws.s3.folder.folderName1}")
    private String bucketFolder;

    @Transactional
    public void registerScript(final UserEntity user, MultipartFile[] files) throws IOException {
        try {
            UserEntity userInfo = userRepo.findById(user.getId());

            if(userInfo == null)
                throw new RuntimeException("로그인이 필요한 서비스입니다.");

            String filePath = uploadScript(files, userInfo.getNickname());

            String normalizedTitle = Normalizer.normalize(FilenameUtils.getBaseName(files[0].getOriginalFilename()), Normalizer.Form.NFKC);

            ProductEntity script = ProductEntity.builder()
                    .title(normalizedTitle)
                    .writer(user.getNickname())
                    .filePath(filePath)
                    .checked(ProductStatus.WAIT)
                    .user(userInfo)
                    .build();

            fileRepo.save(script);

            mailSendService.joinRegisterEmail(userInfo.getEmail(), normalizedTitle);
        } catch (Exception e) {
            throw e;
        }
    }

    // ================ private (protected) method =====================

    protected String uploadScript(MultipartFile[] files, String writer) throws IOException {
        if(files[0].isEmpty())
            throw new RuntimeException("선택된 파일이 없음");
        else if(files.length > 1)
            throw new RuntimeException("파일 수가 1개를 초과함");

        if (!Objects.equals(files[0].getContentType(), "application/pdf"))
            throw new RuntimeException("contentType is not PDF");

        // 파일 이름 가공
        final SimpleDateFormat dateFormat = new SimpleDateFormat( "yyyyMMddHHmmss");
        final Date time = new Date();
        final String name = files[0].getOriginalFilename();
        final String[] fileName = new String[]{Objects.requireNonNull(name).substring(0, name.length() - 4)};

        // S3 Key 구성
        final String S3Key = bucketFolder + fileName[0] +"/"+ writer + "/" + dateFormat.format(time) + ".zip";

        // PDF 파일을 zip으로 압축
        byte[] zippedBytes = compressToZip(files[0]);

        final ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(zippedBytes.length);
        metadata.setContentType("application/pdf");

        try (InputStream inputStream = new ByteArrayInputStream(zippedBytes)) {
            amazonS3.putObject(bucket, S3Key, inputStream, metadata);
        }

        return S3Key;
    }

    private static byte[] compressToZip(MultipartFile file) throws IOException {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream)) {
            ZipEntry zipEntry = new ZipEntry(Objects.requireNonNull(file.getOriginalFilename()));
            zipOutputStream.putNextEntry(zipEntry);
            zipOutputStream.write(file.getBytes());
            zipOutputStream.closeEntry();
            zipOutputStream.flush();

            return outputStream.toByteArray();
        }
    }
}
