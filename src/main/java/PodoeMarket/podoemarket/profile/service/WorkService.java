package PodoeMarket.podoemarket.profile.service;

import PodoeMarket.podoemarket.common.entity.ProductEntity;
import PodoeMarket.podoemarket.common.entity.type.ProductStatus;
import PodoeMarket.podoemarket.common.repository.ProductRepository;
import PodoeMarket.podoemarket.profile.dto.response.ScriptDetailResponseDTO;
import PodoeMarket.podoemarket.profile.dto.response.WorkListResponseDTO;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CopyObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Slf4j
@Service
public class WorkService {
    private final ProductRepository productRepo;

    private final AmazonS3 amazonS3;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    @Value("${cloud.aws.s3.folder.folderName3}")
    private String descriptionBucketFolder;

    @Value("${cloud.aws.s3.folder.folderName2}")
    private String scriptImageBucketFolder;

    @Value("${cloud.aws.s3.url}")
    private String bucketURL;

    public List<WorkListResponseDTO.DateWorkDTO> getDateWorks(final UUID userId) throws UnsupportedEncodingException {
        final List<ProductEntity> products = productRepo.findAllByUserId(userId);

        if (products.isEmpty())
            return Collections.emptyList();

        final Map<LocalDate, List<WorkListResponseDTO.DateWorkDTO.WorksDTO>> works = new HashMap<>();

        for (final ProductEntity product : products) {
            final WorkListResponseDTO.DateWorkDTO.WorksDTO worksDTO = WorkListResponseDTO.DateWorkDTO.WorksDTO.builder()
                    .id(product.getId())
                    .title(product.getTitle())
                    .imagePath(product.getImagePath() != null ? bucketURL + URLEncoder.encode(product.getImagePath(), "UTF-8") : "")
                    .script(product.getScript())
                    .scriptPrice(product.getScriptPrice())
                    .performance(product.getPerformance())
                    .performancePrice(product.getPerformancePrice())
                    .checked(product.getChecked())
                    .build();
            final LocalDate date = product.getCreatedAt().toLocalDate();

            // 날짜에 따른 리스트를 초기화하고 추가 - date라는 key가 없으면 만들고, worksDTO를 value로 추가
            works.computeIfAbsent(date, k -> new ArrayList<>()).add(worksDTO);
        }

        return works.entrySet().stream()
                .sorted(Map.Entry.<LocalDate, List<WorkListResponseDTO.DateWorkDTO.WorksDTO>>comparingByKey().reversed()) // 최신 날짜부터 정렬
                .map(entry -> new WorkListResponseDTO.DateWorkDTO(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    public ScriptDetailResponseDTO getProductDetail(final UUID productId, int buyStatus) throws UnsupportedEncodingException {
        final ProductEntity script = productRepo.findById(productId);

        ScriptDetailResponseDTO scriptDetailDTO = new ScriptDetailResponseDTO();

        String encodedScriptImage = script.getImagePath() != null ? bucketURL + URLEncoder.encode(script.getImagePath(), "UTF-8") : "";
        String encodedDescription = script.getDescriptionPath() != null ? bucketURL + URLEncoder.encode(script.getDescriptionPath(), "UTF-8") : "";

        scriptDetailDTO.setId(script.getId());
        scriptDetailDTO.setTitle(script.getTitle());
        scriptDetailDTO.setWriter(script.getWriter());
        scriptDetailDTO.setImagePath(encodedScriptImage);
        scriptDetailDTO.setScript(script.getScript());
        scriptDetailDTO.setScriptPrice(script.getScriptPrice());
        scriptDetailDTO.setPerformance(script.getPerformance());
        scriptDetailDTO.setPerformancePrice(script.getPerformancePrice());
        scriptDetailDTO.setDescriptionPath(encodedDescription);
        scriptDetailDTO.setDate(script.getCreatedAt());
        scriptDetailDTO.setChecked(script.getChecked());
        scriptDetailDTO.setPlayType(script.getPlayType());
        scriptDetailDTO.setPlot(script.getPlot());

        scriptDetailDTO.setBuyStatus(buyStatus);

        scriptDetailDTO.setAny(script.getAny());
        scriptDetailDTO.setMale(script.getMale());
        scriptDetailDTO.setFemale(script.getFemale());
        scriptDetailDTO.setStageComment(script.getStageComment());
        scriptDetailDTO.setRunningTime(script.getRunningTime());
        scriptDetailDTO.setScene(script.getScene());
        scriptDetailDTO.setAct(script.getAct());

        return scriptDetailDTO;
    }

    @Transactional
    public void productUpdate(final UUID id, final ProductEntity productEntity) {
        final ProductEntity product = productRepo.findById(id);

        if(product.getChecked() == ProductStatus.WAIT)
            throw new RuntimeException("등록 심사 중인 작품");

        product.setImagePath(productEntity.getImagePath());
        product.setTitle(productEntity.getTitle());
        product.setScript(productEntity.getScript());
        product.setPerformance(productEntity.getPerformance());
        product.setScriptPrice(productEntity.getScriptPrice());
        product.setPerformancePrice(productEntity.getPerformancePrice());
        product.setScriptPrice(productEntity.getScriptPrice());
        product.setPerformancePrice(productEntity.getPerformancePrice());
        product.setDescriptionPath(productEntity.getDescriptionPath());
        product.setPlot(productEntity.getPlot());

        if (productEntity.getAny() < 0 || productEntity.getMale() < 0 || productEntity.getFemale() < 0)
            throw new RuntimeException("등장인물이 0명 이상이어야 함");

        if(productEntity.getStageComment() == null)
            throw new RuntimeException("무대 설명이 작성되어야 함");

        if(productEntity.getRunningTime() <= 0)
            throw new RuntimeException("공연 시간이 0분 이상이어야 함");

        if(productEntity.getScene() < 0 || productEntity.getAct() < 0)
            throw new RuntimeException("장과 막이 작성되어야 함");

        product.setAny(productEntity.getAny());
        product.setMale(productEntity.getMale());
        product.setFemale(productEntity.getFemale());
        product.setStageComment(productEntity.getStageComment());
        product.setRunningTime(productEntity.getRunningTime());
        product.setScene(productEntity.getScene());
        product.setAct(productEntity.getAct());

        productRepo.save(product);
    }

    @Transactional
    public String uploadScriptImage(final MultipartFile[] files, final String title, final UUID id) throws IOException {
        if(files.length > 1) {
            throw new RuntimeException("작품 이미지가 1개를 초과함");
        }

        if(!Objects.equals(files[0].getContentType(), "image/jpeg") && !Objects.equals(files[0].getContentType(), "image/jpeg") && !Objects.equals(files[0].getContentType(), "image/png")) {
            throw new RuntimeException("ScriptImage file type is only jpg and png");
        }

        // 파일 이름 가공
        final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
        final Date time = new Date();
        final String name = files[0].getOriginalFilename();
        final String[] fileName = new String[]{Objects.requireNonNull(name).substring(0, name.length() - 4)};

        // S3 Key 구성
        final String S3Key = scriptImageBucketFolder + fileName[0] + "\\" + title + "\\" + dateFormat.format(time) + ".jpg";

        final ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(files[0].getSize());
        metadata.setContentType(files[0].getContentType());

        // 기존 파일 삭제
        if(productRepo.findById(id).getImagePath() != null) {
            final String sourceKey = productRepo.findById(id).getImagePath();

            deleteFile(bucket, sourceKey);
        }

        // 저장
        amazonS3.putObject(bucket, S3Key, files[0].getInputStream(), metadata);

        return S3Key;
    }

    @Transactional
    public String uploadDescription(final MultipartFile[] files, final String title, final UUID id) throws IOException {
        if(files.length > 1) {
            throw new RuntimeException("작품 설명 파일 수가 1개를 초과함");
        }

        if(!Objects.equals(files[0].getContentType(), "application/pdf") && !Objects.equals(files[0].getContentType(), "application/pdf")) {
            throw new RuntimeException("Description file type is not PDF");
        }

        try (InputStream inputStream = files[0].getInputStream()) {
            final PdfDocument doc = new PdfDocument(new PdfReader(inputStream));

            if(doc.getNumberOfPages() > 5)
                throw new RuntimeException("작품 설명 파일이 5페이지를 초과함");
        }

        // 파일 이름 가공
        final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
        final Date time = new Date();
        final String name = files[0].getOriginalFilename();
        final String[] fileName = new String[]{Objects.requireNonNull(name).substring(0, name.length() - 4)};

        // S3 Key 구성
        final String S3Key = descriptionBucketFolder + fileName[0] + "\\" + title + "\\" + dateFormat.format(time) + ".pdf";

        final ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(files[0].getSize());
        metadata.setContentType("application/pdf");

        // 기존 파일 삭제
        if(productRepo.findById(id).getDescriptionPath() != null) {
            final String sourceKey = productRepo.findById(id).getDescriptionPath();

            deleteFile(bucket, sourceKey);
        }

        // 저장
        amazonS3.putObject(bucket, S3Key, files[0].getInputStream(), metadata);

        return S3Key;
    }

    public void setScriptImageDefault(final UUID productId) {
        final ProductEntity product = productRepo.findById(productId);

        if(product.getImagePath() != null) {
            final String imagePath = product.getImagePath().replace("scriptImage", "delete");
            moveFile(bucket, product.getImagePath(), imagePath);
            deleteFile(bucket, product.getImagePath());
        }
    }

    public void setDescriptionDefault(final UUID productId) {
        final ProductEntity product = productRepo.findById(productId);

        if(product.getDescriptionPath() != null) {
            final String descriptionPath = product.getDescriptionPath().replace("description", "delete");
            moveFile(bucket, product.getDescriptionPath(), descriptionPath);
            deleteFile(bucket, product.getDescriptionPath());
        }
    }

    // ============= private method ===============
    public String extractS3KeyFromURL(final String S3URL) throws Exception {
        String decodedUrl = URLDecoder.decode(S3URL, StandardCharsets.UTF_8);
        final URL url = new URL(decodedUrl);

        return url.getPath().startsWith("/") ? url.getPath().substring(1) : url.getPath();
    }

    private void deleteFile(final String bucket, final String sourceKey) {
        if(amazonS3.doesObjectExist(bucket, sourceKey))
            amazonS3.deleteObject(bucket, sourceKey);
    }

    private void moveFile(final String bucket, final String sourceKey, final String destinationKey) {
        final CopyObjectRequest copyFile = new CopyObjectRequest(bucket,sourceKey, bucket, destinationKey);

        if(amazonS3.doesObjectExist(bucket, sourceKey))
            amazonS3.copyObject(copyFile);
    }
}
