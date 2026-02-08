package PodoeMarket.podoemarket.performance.service;

import PodoeMarket.podoemarket.common.entity.PerformanceEntity;
import PodoeMarket.podoemarket.common.entity.UserEntity;
import PodoeMarket.podoemarket.common.entity.type.PerformanceStatus;
import PodoeMarket.podoemarket.common.repository.PerformanceRepository;
import PodoeMarket.podoemarket.performance.dto.request.PerformanceRegisterRequestDTO;
import PodoeMarket.podoemarket.performance.dto.request.PerformanceUpdateRequestDTO;
import PodoeMarket.podoemarket.performance.dto.response.PerformanceEditResponseDTO;
import PodoeMarket.podoemarket.performance.dto.response.PerformanceMainResponseDTO;
import PodoeMarket.podoemarket.performance.dto.response.PerformanceStatusResponseDTO;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.*;
import java.time.ZoneId;
import java.util.List;

@RequiredArgsConstructor
@Slf4j
@Service
@Transactional(readOnly = true)
public class PerformanceService {
    private final AmazonS3 amazonS3;
    private final PerformanceRepository performanceRepo;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    @Value("${cloud.aws.s3.folder.folderName5}")
    private String posterBucketFolder;

    @Value("${cloud.aws.s3.url}")
    private String bucketURL;

    @Transactional
    public void uploadPerformanceInfo(UserEntity userInfo, PerformanceRegisterRequestDTO dto, MultipartFile file) throws IOException {
        try {
            if(userInfo == null)
                throw new RuntimeException("로그인이 필요한 서비스입니다.");

            // 입력 받은 제목을 NFKC 정규화 적용 (전각/반각, 분해형/조합형 등 모든 호환성 문자를 통일)
            String normalizedTitle = Normalizer.normalize(dto.getTitle(), Normalizer.Form.NFKC);

            if(normalizedTitle == null || normalizedTitle.isBlank())
                throw new RuntimeException("공연 제목은 필수 입력 요소입니다.");

            if(dto.getPlace() == null || dto.getPlace().isBlank())
                throw new RuntimeException("공연 장소는 필수 입력 요소입니다.");

            if(dto.getStartDate() == null || dto.getEndDate() == null)
                throw new RuntimeException("공연 날짜는 필수 입력 요소입니다.");

            if(dto.getEndDate().isBefore(dto.getStartDate()))
                throw new RuntimeException("공연 종료일은 공연 시작일과 같거나 이후여야 합니다.");

            if(dto.getLink() == null || dto.getLink().isBlank())
                throw new RuntimeException("공연 정보 바로가기 링크는 필수 입력 요소입니다.");

            if(file == null || file.getSize() <= 0)
                throw new RuntimeException("공연 포스터는 필수 요소 입니다.");

            String posterPath = uploadPoster(file, dto.getTitle());

            final PerformanceEntity performance = PerformanceEntity.builder()
                    .user(userInfo)
                    .posterPath(posterPath)
                    .title(normalizedTitle)
                    .place(dto.getPlace())
                    .startDate(dto.getStartDate())
                    .endDate(dto.getEndDate())
                    .link(dto.getLink())
                    .isUsed(dto.getIsUsed())
                    .build();

            performanceRepo.save(performance);
        } catch (Exception e) {
            throw e;
        }
    }


    public PerformanceEditResponseDTO getPerformanceInfo(UserEntity userInfo, UUID id) {
        try {
            final PerformanceEntity performance = performanceRepo.findById(id);

            if(performance == null)
                throw new RuntimeException("해당하는 공연 소식이 없습니다.");

            String posterPath = performance.getPosterPath() != null
                    ? bucketURL + URLEncoder.encode(performance.getPosterPath(), StandardCharsets.UTF_8)
                    : "";

            return PerformanceEditResponseDTO.builder()
                    .isOwner(userInfo != null && performance.getUser().getId().equals(userInfo.getId()))
                    .posterPath(posterPath)
                    .title(performance.getTitle())
                    .place(performance.getPlace())
                    .startDate(performance.getStartDate())
                    .endDate(performance.getEndDate())
                    .link(performance.getLink())
                    .isUsed(performance.getIsUsed())
                    .build();
        } catch (Exception e) {
            throw e;
        }
    }

    @Transactional
    public void deletePerformanceInfo(UserEntity userInfo, UUID id) {
        try {
            final PerformanceEntity performance = performanceRepo.findById(id);

            if(performance == null)
                throw new RuntimeException("해당하는 공연 소식이 없습니다.");

            if(!performance.getUser().getId().equals(userInfo.getId()))
                throw new RuntimeException("삭제 권한이 없습니다.");

            deleteFile(bucket, performance.getPosterPath());

            performanceRepo.delete(performance);
        } catch (Exception e) {
            throw e;
        }
    }

    @Transactional
    public void updatePerformanceInfo(UserEntity userInfo, UUID id, PerformanceUpdateRequestDTO dto, MultipartFile file) throws IOException {
        try {
            final PerformanceEntity performance = performanceRepo.findById(id);

            if(performance == null)
                throw new RuntimeException("해당하는 공연 소식이 없습니다.");

            if(!performance.getUser().getId().equals(userInfo.getId()))
                throw new RuntimeException("수정 권한이 없습니다.");

            if(file != null && !file.isEmpty()) {
                deleteFile(bucket, performance.getPosterPath());
                performance.setPosterPath(uploadPoster(file, dto.getTitle()));
            }

            if(dto.getTitle() != null && !dto.getTitle().isBlank())
                performance.setTitle(dto.getTitle());

            if(dto.getPlace() != null && !dto.getPlace().isBlank())
                performance.setPlace(dto.getPlace());

            if(dto.getStartDate() != null && dto.getEndDate() != null)
                if (dto.getEndDate().isBefore(dto.getStartDate()))
                    throw new RuntimeException("공연 종료일은 공연 시작일과 같거나 이후여야 합니다.");

            if(dto.getStartDate() != null)
                performance.setStartDate(dto.getStartDate());

            if(dto.getEndDate() != null)
                performance.setEndDate(dto.getEndDate());

            if(dto.getLink() != null && !dto.getLink().isBlank())
                performance.setLink(dto.getLink());

            if(dto.getIsUsed() != null)
                performance.setIsUsed(dto.getIsUsed());

            performanceRepo.save(performance);
        } catch (Exception e) {
            throw e;
        }
    }

    public List<PerformanceMainResponseDTO> getStatusPerformanceMain(PerformanceStatus status, Boolean isUsed, UserEntity userInfo) {
        try {
            LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
            Pageable limit4 = PageRequest.of(0, 4, Sort.by("startDate").descending());

            Page<PerformanceEntity> page = switch(status) {
                case ONGOING -> performanceRepo.findOngoing(today, isUsed, limit4);
                case UPCOMING ->  performanceRepo.findUpcoming(today, isUsed, limit4);
                case PAST ->  performanceRepo.findPast(today, isUsed, limit4);
            };

            return page.getContent()
                    .stream()
                    .map(performance -> getMainStatusListDTO(performance, userInfo))
                    .toList();
        } catch (Exception e) {
            throw e;
        }
    }

    public Page<PerformanceStatusResponseDTO.PerformanceListDTO> getPerformanceList(UserEntity userInfo, PerformanceStatus status, Boolean isUsed, int page, int pageSize) {
        try {
            LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
            Pageable pageable = PageRequest.of(page, pageSize, Sort.by("startDate").descending());

            Page<PerformanceEntity> result;

            switch(status) {
                case ONGOING -> result = performanceRepo.findOngoing(today, isUsed, pageable);

                case UPCOMING -> result =
                        performanceRepo.findUpcoming(today, isUsed, pageable);

                case PAST -> result =
                        performanceRepo.findPast(today, isUsed, pageable);

                default -> throw new IllegalArgumentException("잘못된 공연 상태입니다.");
            }

            return result.map(p -> getStatusListDTO(p, userInfo));
        } catch (Exception e) {
            throw e;
        }
    }

    // ============= private method ===============

    private void deleteFile(final String bucket, final String sourceKey) {
        try {
            if(amazonS3.doesObjectExist(bucket, sourceKey))
                amazonS3.deleteObject(bucket, sourceKey);
        } catch (Exception e) {
            throw new RuntimeException("파일 삭제 실패", e);
        }
    }

    protected String uploadPoster(final MultipartFile file, final String title) throws IOException {
        try {
            // MIME 타입 체크
            String contentType = file.getContentType();
            if (!"image/jpeg".equals(contentType) &&
                    !"image/jpg".equals(contentType) &&
                    !"image/png".equals(contentType)) {
                throw new RuntimeException("이미지 파일은 jpg 또는 png만 허용됩니다.");
            }

            // 용량 확인
            long maxSize = 15L * 1024 * 1024; // 15MB
            if(file.getSize() > maxSize)
                throw new RuntimeException("이미지 파일은 최대 15MB까지만 업로드할 수 있습니다.");

            // S3 Key 구성
            final String date = new SimpleDateFormat("yyyyMMdd").format(new Date());
            final String name = file.getOriginalFilename();
            final String[] fileName = new String[]{Objects.requireNonNull(name).substring(0, name.length() - 4)};
            final String S3Key = posterBucketFolder + fileName[0] + "/" + title + "/" + date + ".jpg";

            // 이미지 압축, 리사이즈 진행 (ex. 품질 0.7 = 70%)
            byte[] compressedImage = compressImage(file, 466, 0.7f);

            final ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(compressedImage.length);
            metadata.setContentType("image/jpeg");

            // 저장
            try (InputStream inputStream = new ByteArrayInputStream(compressedImage)) {
                amazonS3.putObject(bucket, S3Key, inputStream, metadata);
            }

            return S3Key;
        } catch (Exception e) {
            throw e;
        }
    }

    // 모든 이미지를 JPEGWriter가 처리 가능한 RGB Bitmap으로 변환
    private static BufferedImage forceRGB(BufferedImage img) {
        BufferedImage rgbImage = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_3BYTE_BGR);

        Graphics2D g = rgbImage.createGraphics();
        g.setComposite(AlphaComposite.Src);
        g.drawImage(img, 0, 0, Color.WHITE, null);
        g.dispose();

        return rgbImage;
    }

    private static byte[] compressImage(MultipartFile file, int targetSize, float quality) throws IOException {
        BufferedImage originalImage;
        try (InputStream in = file.getInputStream()) {
            originalImage = ImageIO.read(in);

            if (originalImage == null)
                throw new IOException("이미지 읽기 실패");
        }

        // JPEGWriter가 안전하게 처리할 수 있도록 복잡한 color model(PNG 등) → BGR RGB로 완전히 변환
        BufferedImage rbgImage = forceRGB(originalImage);

        // 원본 메모리 해제 (대형 PNG 7000px 이상일 때 필수)
        originalImage.flush();

        // 리사이즈
        Image scaled = rbgImage.getScaledInstance(targetSize, targetSize, Image.SCALE_SMOOTH);
        BufferedImage resizedBuffered = new BufferedImage(targetSize, targetSize, BufferedImage.TYPE_3BYTE_BGR);

        Graphics2D g2d = resizedBuffered.createGraphics();
        g2d.drawImage(scaled, 0, 0, null);
        g2d.dispose();

        // JPEG 품질 압축
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();
        ImageWriteParam writeParam = writer.getDefaultWriteParam();

        writeParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        writeParam.setCompressionQuality(quality); // 0.0 ~ 1.0 (낮을수록 압축률↑, 용량↓, 화질↓)

        try (ImageOutputStream imgOutputStream = ImageIO.createImageOutputStream(outputStream)) {
            writer.setOutput(imgOutputStream);
            writer.write(null, new IIOImage(rbgImage, null, null), writeParam);
        } finally {
            writer.dispose();
        }

        return outputStream.toByteArray();
    }

    private PerformanceMainResponseDTO getMainStatusListDTO(PerformanceEntity performance, UserEntity userInfo) {
        String posterPath = performance.getPosterPath() != null
                ? bucketURL + URLEncoder.encode(performance.getPosterPath(), StandardCharsets.UTF_8)
                : "";

        boolean isOwner = userInfo != null && performance.getUser() != null && performance.getUser().getId().equals(userInfo.getId());

        return PerformanceMainResponseDTO.builder()
                .id(performance.getId())
                .posterPath(posterPath)
                .title(performance.getTitle())
                .place(performance.getPlace())
                .startDate(performance.getStartDate())
                .endDate(performance.getEndDate())
                .isUsed(performance.getIsUsed())
                .isOwner(isOwner)
                .link(performance.getLink())
                .build();
    }

    private PerformanceStatusResponseDTO.PerformanceListDTO getStatusListDTO(PerformanceEntity p, UserEntity userInfo) {
        String posterPath = p.getPosterPath() != null
                ? bucketURL + URLEncoder.encode(p.getPosterPath(), StandardCharsets.UTF_8)
                : "";

        boolean isOwner = userInfo != null && p.getUser() != null && p.getUser().getId().equals(userInfo.getId());

        return PerformanceStatusResponseDTO.PerformanceListDTO.builder()
                .id(p.getId())
                .posterPath(posterPath)
                .title(p.getTitle())
                .place(p.getPlace())
                .startDate(p.getStartDate())
                .endDate(p.getEndDate())
                .isUsed(p.getIsUsed())
                .isOwner(isOwner)
                .link(p.getLink())
                .build();
    }
}
