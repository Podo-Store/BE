package PodoeMarket.podoemarket.profile.service;

import PodoeMarket.podoemarket.common.entity.ProductEntity;
import PodoeMarket.podoemarket.common.entity.UserEntity;
import PodoeMarket.podoemarket.common.entity.type.ProductStatus;
import PodoeMarket.podoemarket.common.entity.type.StageType;
import PodoeMarket.podoemarket.common.repository.ProductRepository;
import PodoeMarket.podoemarket.profile.dto.request.DetailUpdateRequestDTO;
import PodoeMarket.podoemarket.profile.dto.response.ScriptDetailResponseDTO;
import PodoeMarket.podoemarket.profile.dto.response.WorkListResponseDTO;
import PodoeMarket.podoemarket.service.MailSendService;
import PodoeMarket.podoemarket.service.S3Service;
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
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.io.*;
import java.text.Normalizer;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@RequiredArgsConstructor
@Slf4j
@Service
@Transactional(readOnly = true)
public class WorkService {
    private final ProductRepository productRepo;
    private final AmazonS3 amazonS3;
    private final S3Service s3Service;
    private final MailSendService mailSendService;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    @Value("${cloud.aws.s3.folder.folderName3}")
    private String descriptionBucketFolder;

    @Value("${cloud.aws.s3.folder.folderName2}")
    private String scriptImageBucketFolder;

    @Value("${cloud.aws.s3.folder.folderName1}")
    private String scriptBucketFolder;

    public WorkListResponseDTO getUserWorks(final UserEntity userInfo) {
        try {
            final List<ProductEntity> products = productRepo.findAllByUserId(userInfo.getId());

            // 날짜별로 주문 항목을 그룹화하기 위한 맵 선언
            final Map<LocalDate, List<WorkListResponseDTO.DateWorkDTO.WorksDTO>> works = new HashMap<>();

            for (final ProductEntity product : products) {
                if (!product.getIsDelete()) {
                    String scriptImage = product.getImagePath() != null ? s3Service.generatePreSignedURL(product.getImagePath()) : "";

                    final WorkListResponseDTO.DateWorkDTO.WorksDTO worksDTO = WorkListResponseDTO.DateWorkDTO.WorksDTO.builder()
                            .id(product.getId())
                            .title(product.getTitle())
                            .imagePath(scriptImage)
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
            }

            List<WorkListResponseDTO.DateWorkDTO> dateWorks = works.entrySet().stream()
                    .sorted(Map.Entry.<LocalDate, List<WorkListResponseDTO.DateWorkDTO.WorksDTO>>comparingByKey().reversed()) // 최신 날짜부터 정렬
                    .map(entry -> new WorkListResponseDTO.DateWorkDTO(entry.getKey(), entry.getValue()))
                    .toList();

            return WorkListResponseDTO.builder()
                    .nickname(userInfo.getNickname())
                    .dateWorks(dateWorks)
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("사용자 작품 목록 조회 실패", e);
        }
    }

    public ScriptDetailResponseDTO getProductDetail(final UUID productId, int buyStatus) {
        try {
            final ProductEntity script = productRepo.findById(productId);

            if(script == null)
                throw new RuntimeException("상품을 찾을 수 없습니다.");

            ScriptDetailResponseDTO scriptDetailDTO = new ScriptDetailResponseDTO();
            String scriptImage = script.getImagePath() != null
                    ? s3Service.generatePreSignedURL(script.getImagePath())
                    : "";
            String descriptionPath = script.getDescriptionPath();
            String descriptionTitle = (StringUtils.hasText(descriptionPath) && descriptionPath.contains("/"))
                    ? descriptionPath.split("/")[1]
                    : "";

            scriptDetailDTO.setId(script.getId());
            scriptDetailDTO.setTitle(script.getTitle());
            scriptDetailDTO.setWriter(script.getWriter());
            scriptDetailDTO.setImagePath(scriptImage);
            scriptDetailDTO.setScript(script.getScript());
            scriptDetailDTO.setScriptPrice(script.getScriptPrice());
            scriptDetailDTO.setPerformance(script.getPerformance());
            scriptDetailDTO.setPerformancePrice(script.getPerformancePrice());
            scriptDetailDTO.setDescriptionPath(descriptionTitle);
            scriptDetailDTO.setDate(script.getCreatedAt());
            scriptDetailDTO.setChecked(script.getChecked());
            scriptDetailDTO.setPlayType(script.getPlayType());
            scriptDetailDTO.setPlot(script.getPlot());
            scriptDetailDTO.setIntention(script.getIntention());

            scriptDetailDTO.setBuyStatus(buyStatus);

            scriptDetailDTO.setAny(script.getAny());
            scriptDetailDTO.setMale(script.getMale());
            scriptDetailDTO.setFemale(script.getFemale());
            scriptDetailDTO.setStageComment(script.getStageComment());
            scriptDetailDTO.setRunningTime(script.getRunningTime());
            scriptDetailDTO.setScene(script.getScene());
            scriptDetailDTO.setAct(script.getAct());

            return scriptDetailDTO;
        } catch (Exception e) {
            throw e;
        }
    }

    @Transactional
    public void updateProductDetail(UserEntity userInfo, DetailUpdateRequestDTO dto, MultipartFile[] file1, MultipartFile[] file2) throws IOException {
        try {
            // 입력 받은 제목을 NFKC 정규화 적용 (전각/반각, 분해형/조합형 등 모든 호환성 문자를 통일)
            String normalizedTitle = Normalizer.normalize(dto.getTitle(), Normalizer.Form.NFKC);

            if(!isValidTitle(normalizedTitle))
                throw new RuntimeException("제목 유효성 검사 실패");

            if(!isValidPlot(dto.getPlot()))
                throw new RuntimeException("줄거리 유효성 검사 실패");

            if(dto.getAny() < 0 || dto.getMale() < 0 || dto.getFemale() < 0)
                throw new RuntimeException("등장인물이 0명 이상이어야 함");

            if(dto.getStageComment() == null)
                throw new RuntimeException("무대 설명이 작성되어야 함");

            if(dto.getRunningTime() <= 0)
                throw new RuntimeException("공연 시간이 0분 이상이어야 함");

            if(dto.getScene() < 0 || dto.getAct() < 0)
                throw new RuntimeException("장과 막이 작성되어야 함");

            if(!isValidIntention(dto.getIntention()))
                throw new RuntimeException("작가 의도 300자 초과");

            // 스테이지 별 설정 조건
            if (userInfo.getStageType() == StageType.SINGLE_GRAPE && dto.getScriptPrice() != 0)
                throw new RuntimeException("포도알 스테이지에서의 대본 가격은 무료로만 설정 가능합니다.");

            final ProductEntity product = productRepo.findById(dto.getId());

            if(product == null)
                throw new RuntimeException("상품을 찾을 수 없습니다.");

            if(product.getChecked() == ProductStatus.WAIT)
                throw new RuntimeException("등록 심사 중인 작품");

            String scriptImageFilePath = null;
            if(file1 != null && file1.length > 0 && !file1[0].isEmpty())
                scriptImageFilePath = uploadScriptImage(file1, dto.getTitle(), dto.getId());
            else if (dto.getImagePath() != null)
                scriptImageFilePath = product.getImagePath();
            else {
                if(product.getImagePath() != null) {
                    final String imagePath = product.getImagePath().replace("scriptImage", "delete");
                    moveFile(bucket, product.getImagePath(), imagePath);
                    deleteFile(bucket, product.getImagePath());
                }
            }

            String descriptionFilePath = null;
            if(file2 != null && file2.length > 0 && !file2[0].isEmpty()) // 설명 파일을 새로 업로드한 경우 (기존 파일이 있든 없든 새로 교체함)
                descriptionFilePath = uploadDescription(file2, dto.getTitle(), dto.getId());
            else if (dto.getDescriptionPath() != null) // 파일을 변경하지 않는 경우
                descriptionFilePath = product.getDescriptionPath();
            else { // 기존에 존재하던 설명 파일을 삭제하는 경우
                if (product.getDescriptionPath() != null) {
                    final String descriptionPath = product.getDescriptionPath().replace("description", "delete");
                    moveFile(bucket, product.getDescriptionPath(), descriptionPath);
                    deleteFile(bucket, product.getDescriptionPath());
                }
            }

            product.setImagePath(scriptImageFilePath);
            product.setTitle(normalizedTitle);
            product.setScript(dto.getScript());
            product.setPerformance(dto.getPerformance());
            product.setScriptPrice(dto.getScriptPrice());
            product.setPerformancePrice(dto.getPerformancePrice());
            product.setDescriptionPath(descriptionFilePath);
            product.setPlot(dto.getPlot());
            product.setIntention(dto.getIntention());

            // 개요
            product.setAny(dto.getAny());
            product.setMale(dto.getMale());
            product.setFemale(dto.getFemale());
            product.setStageComment(dto.getStageComment());
            product.setRunningTime(dto.getRunningTime());
            product.setScene(dto.getScene());
            product.setAct(dto.getAct());

            productRepo.save(product);
        } catch (Exception e) {
            throw e;
        }
    }

    @Transactional
    public void deleteProduct(final UUID productId, final UUID userId) {
        try {
            final ProductEntity product = productRepo.findById(productId);

            if(product == null)
                throw new RuntimeException("작품을 찾을 수 없습니다.");

            if(!product.getUser().getId().equals(userId))
                throw new RuntimeException("작가가 아닙니다.");

            if(product.getChecked() == ProductStatus.WAIT)
                throw new RuntimeException("심사 중");

            // 탈퇴와 동일한 파일 삭제 처리 필요
            deleteScripts(product);
        } catch (Exception e) {
            throw e;
        }
    }

    @Transactional
    public void cancelRegister(final UUID productId, final UUID userId) {
        try {
            final ProductEntity product = productRepo.findById(productId);

            if(product == null)
                throw new RuntimeException("작품을 찾을 수 없습니다.");

            if(!product.getUser().getId().equals(userId))
                throw new RuntimeException("작가가 아닙니다.");

            if(product.getChecked() != ProductStatus.WAIT)
                throw new RuntimeException("심사 중이 아닙니다.");

            deleteFile(bucket, product.getFilePath());

            productRepo.delete(product);
        } catch (Exception e) {
            throw e;
        }
    }

    @Transactional
    public void changeScript(final UUID productId, final UserEntity user, MultipartFile file) throws IOException {
        final ProductEntity product = productRepo.findById(productId);

        if(product == null)
            throw new RuntimeException("작품을 찾을 수 없습니다.");

        if(!product.getUser().getId().equals(user.getId()))
            throw new RuntimeException("작가가 아닙니다.");

        if(product.getChecked() == ProductStatus.RE_WAIT)
            throw new RuntimeException("재심사 결과 대기 중에는 재신청이 불가합니다.");

        // 파일 처리 필요
        String tempFilePath = uploadScript(file, user.getNickname());

        product.setTempFilePath(tempFilePath);
        product.setChecked(ProductStatus.RE_WAIT);
        product.setResubmittedAt(LocalDateTime.now(ZoneId.of("Asia/Seoul")));
        productRepo.save(product);

        // 메일 전송 로직 필요
        mailSendService.joinReviewEmail(user.getEmail(), product.getTitle());
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

    private void moveFile(final String bucket, final String sourceKey, final String destinationKey) {
        try {
            final CopyObjectRequest copyFile = new CopyObjectRequest(bucket,sourceKey, bucket, destinationKey);

            if(amazonS3.doesObjectExist(bucket, sourceKey))
                amazonS3.copyObject(copyFile);
        } catch (Exception e) {
            throw new RuntimeException("파일 이동 실패", e);
        }
    }

    protected String uploadScriptImage(final MultipartFile[] files, final String title, final UUID id) throws IOException {
        try {
            if(files.length > 1)
                throw new RuntimeException("작품 이미지가 1개를 초과함");

            MultipartFile file = files[0];

            // MIME 타입 체크
            String contentType = file.getContentType();
            if (!"image/jpeg".equals(contentType) &&
                    !"image/jpg".equals(contentType) &&
                    !"image/png".equals(contentType)) {
                throw new RuntimeException("이미지 파일은 jpg 또는 png만 허용됩니다.");
            }

            // S3 Key 구성
            final String date = new SimpleDateFormat("yyyyMMdd").format(new Date());
            final String name = files[0].getOriginalFilename();
            final String[] fileName = new String[]{Objects.requireNonNull(name).substring(0, name.length() - 4)};
            final String S3Key = scriptImageBucketFolder + fileName[0] + "/" + title + "/" + date + ".jpg";

            // 기존 파일 삭제
            final ProductEntity product = productRepo.findById(id);

            if(product == null)
                throw new RuntimeException("상품을 찾을 수 없습니다.");

            if(product.getImagePath() != null)
                deleteFile(bucket, product.getImagePath());

            // 이미지 압축, 리사이즈 진행 (ex. 품질 0.7 = 70%)
            byte[] compressedImage = compressImage(files[0], 466, 0.7f);

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

    protected String uploadDescription(final MultipartFile[] files, final String title, final UUID id) throws IOException {
        try {
            if(files.length > 1)
                throw new RuntimeException("작품 설명 파일 수가 1개를 초과함");

            if(!Objects.equals(files[0].getContentType(), "application/pdf"))
                throw new RuntimeException("Description file type is not PDF");

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
            final String S3Key = descriptionBucketFolder + fileName[0] + "/" + title + "/" + dateFormat.format(time) + ".zip";

            // PDF 파일을 zip으로 압축
            byte[] zippedBytes = compressToZip(files[0]);

            final ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(zippedBytes.length);
            metadata.setContentType("application/zip");

            // 기존 파일 삭제
            final ProductEntity product = productRepo.findById(id);

            if(product == null)
                throw new RuntimeException("상품을 찾을 수 없습니다.");

            if(StringUtils.hasText(product.getDescriptionPath()))
                deleteFile(bucket, product.getDescriptionPath());

            // 저장
            try (InputStream inputStream = new ByteArrayInputStream(zippedBytes)) {
                amazonS3.putObject(bucket, S3Key, inputStream, metadata);
            }

            return S3Key;
        } catch (Exception e) {
            throw e;
        }
    }

    protected String uploadScript(MultipartFile file, String writer) throws IOException {
        if(file.isEmpty())
            throw new RuntimeException("선택된 파일이 없음");

        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".pdf"))
            throw new RuntimeException("PDF 파일만 업로드 가능합니다.");

        // 파일 이름 가공
        final SimpleDateFormat dateFormat = new SimpleDateFormat( "yyyyMMddHHmmss");
        final Date time = new Date();
        final String name = file.getOriginalFilename();
        final String[] fileName = new String[]{Objects.requireNonNull(name).substring(0, name.length() - 4)};

        // S3 Key 구성
        final String S3Key = scriptBucketFolder + fileName[0] +"/"+ writer + "/" + dateFormat.format(time) + ".zip";

        // PDF 파일을 zip으로 압축
        byte[] zippedBytes = compressToZip(file);

        final ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(zippedBytes.length);
        metadata.setContentType("application/zip");

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

    private static boolean isValidTitle(String title) {
        if (title == null || title.isBlank()) {
            log.warn("title is null or empty");
            return false;
        }

        // 줄바꿈 정규화: \r\n, \r → \n
        String normalized = title.replace("\r\n", "\n").replace("\r", "");

        // 실제 유니코드 문자 수 (이모지도 1자로 계산됨)
        int charCount = normalized.codePointCount(0, normalized.length());

        if (charCount < 1 || charCount > 20) {
            log.warn("title is not fit in the rule");
            return false;
        }

            log.info("title valid checked");
            return true;
    }

    private static boolean isValidPlot(String plot) {
        if (plot == null || plot.isBlank()) {
            log.warn("plot is null or empty");
            return false;
        }

        // 줄바꿈 정규화: \r\n, \r → \n
        String normalized = plot.replace("\r\n", "\n").replace("\r", "");

        // 실제 유니코드 문자 수 (이모지도 1자로 계산됨)
        int charCount = normalized.codePointCount(0, normalized.length());

        if (charCount < 1 || charCount > 150) {
            log.warn("plot length is invalid: {}", charCount);
            return false;
        }

        log.info("plot valid checked");
        return true;
    }

    private static boolean isValidIntention(String intention) {
        if (intention != null) {
            // 줄바꿈 처리: \r\n 또는 \r 제거하여 모두 \n로 통일
            String normalized = intention.replace("\r\n", "\n").replace("\r", "");

            // 진짜 '문자 수' 기준으로 계산 (이모지도 1자로 계산됨)
            int actualLength = normalized.codePointCount(0, normalized.length());

            return actualLength <= 300;
        } else
            return true;
    }

    private void deleteScripts(final ProductEntity product) {
        try {
            final String filePath = product.getFilePath().replace("script", "delete");
            moveFile(bucket, product.getFilePath(), filePath);
            deleteFile(bucket, product.getFilePath());
            product.setFilePath(filePath);

            product.setImagePath(null);

            if(product.getDescriptionPath() != null) {
                final String descriptionPath = product.getDescriptionPath().replace("description", "delete");
                moveFile(bucket, product.getDescriptionPath(), descriptionPath);
                deleteFile(bucket, product.getDescriptionPath());
                product.setDescriptionPath(descriptionPath);
            }

            product.setIsDelete(true);

            productRepo.save(product);
        } catch (Exception e) {
            throw new RuntimeException("작품 파일 삭제 실패", e);
        }
    }
}
