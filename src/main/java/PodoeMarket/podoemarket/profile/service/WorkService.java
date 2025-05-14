package PodoeMarket.podoemarket.profile.service;

import PodoeMarket.podoemarket.common.entity.ProductEntity;
import PodoeMarket.podoemarket.common.entity.UserEntity;
import PodoeMarket.podoemarket.common.entity.type.ProductStatus;
import PodoeMarket.podoemarket.common.repository.ProductRepository;
import PodoeMarket.podoemarket.profile.dto.request.DetailUpdateRequestDTO;
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

import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.*;
import java.util.regex.Pattern;

@RequiredArgsConstructor
@Slf4j
@Service
@Transactional(readOnly = true)
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

    public WorkListResponseDTO getUserWorks(final UserEntity userInfo) {
        try {
            final List<ProductEntity> products = productRepo.findAllByUserId(userInfo.getId());

            // 날짜별로 주문 항목을 그룹화하기 위한 맵 선언
            final Map<LocalDate, List<WorkListResponseDTO.DateWorkDTO.WorksDTO>> works = new HashMap<>();

            for (final ProductEntity product : products) {
                String encodedScriptImage = product.getImagePath() != null ? bucketURL + URLEncoder.encode(product.getImagePath(), StandardCharsets.UTF_8) : "";

                final WorkListResponseDTO.DateWorkDTO.WorksDTO worksDTO = WorkListResponseDTO.DateWorkDTO.WorksDTO.builder()
                        .id(product.getId())
                        .title(product.getTitle())
                        .imagePath(encodedScriptImage)
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
            String encodedScriptImage = script.getImagePath() != null ? bucketURL + URLEncoder.encode(script.getImagePath(), StandardCharsets.UTF_8) : "";
            String encodedDescription = script.getDescriptionPath() != null ? bucketURL + URLEncoder.encode(script.getDescriptionPath(), StandardCharsets.UTF_8) : "";

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
        } catch (Exception e) {
            throw new RuntimeException("상품 상세 정보 조회 실패", e);
        }
    }

    @Transactional
    public void updateProductDetail(DetailUpdateRequestDTO dto, MultipartFile[] file1, MultipartFile[] file2) {
        try {
            // 입력 받은 제목을 NFKC 정규화 적용 (전각/반각, 분해형/조합형 등 모든 호환성 문자를 통일)
            String normalizedTitle = Normalizer.normalize(dto.getTitle(), Normalizer.Form.NFKC);
            final ProductEntity product = productRepo.findById(dto.getId());

            if(product == null)
                throw new RuntimeException("상품을 찾을 수 업습니다.");

            if(!isValidTitle(normalizedTitle))
                throw new RuntimeException("제목 유효성 검사 실패");

            if(product.getChecked() == ProductStatus.WAIT)
                throw new RuntimeException("등록 심사 중인 작품");

            if(!isValidPlot(dto.getPlot()))
                throw new RuntimeException("줄거리 유효성 검사 실패");

            String scriptImageFilePath = null;
            if(file1 != null && file1.length > 0 && !file1[0].isEmpty())
                scriptImageFilePath = uploadScriptImage(file1, dto.getTitle(), dto.getId());
            else if (dto.getImagePath() != null)
                scriptImageFilePath = extractS3KeyFromURL(dto.getImagePath());
            else {
                if(product.getImagePath() != null) {
                    final String imagePath = product.getImagePath().replace("scriptImage", "delete");
                    moveFile(bucket, product.getImagePath(), imagePath);
                    deleteFile(bucket, product.getImagePath());
                }
            }

            String descriptionFilePath = null;
            if(file2 != null && file2.length > 0 && !file2[0].isEmpty())
                descriptionFilePath = uploadDescription(file2, dto.getTitle(), dto.getId());
            else if (dto.getDescriptionPath() != null)
                descriptionFilePath = extractS3KeyFromURL(dto.getDescriptionPath());
            else {
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

            if (product.getAny() < 0 || product.getMale() < 0 || product.getFemale() < 0)
                throw new RuntimeException("등장인물이 0명 이상이어야 함");

            if(product.getStageComment() == null)
                throw new RuntimeException("무대 설명이 작성되어야 함");

            if(product.getRunningTime() <= 0)
                throw new RuntimeException("공연 시간이 0분 이상이어야 함");

            if(product.getScene() < 0 || product.getAct() < 0)
                throw new RuntimeException("장과 막이 작성되어야 함");

            product.setAny(product.getAny());
            product.setMale(product.getMale());
            product.setFemale(product.getFemale());
            product.setStageComment(product.getStageComment());
            product.setRunningTime(product.getRunningTime());
            product.setScene(product.getScene());
            product.setAct(product.getAct());

            productRepo.save(product);
        } catch (Exception e) {
            throw new RuntimeException("상품 상세 정보 업데이트 실패", e);
        }
    }

    // ============= private method ===============

    private String extractS3KeyFromURL(final String S3URL) {
        try {
            String decodedUrl = URLDecoder.decode(S3URL, StandardCharsets.UTF_8);
            final URL url = (new URI(decodedUrl)).toURL();

            return url.getPath().startsWith("/") ? url.getPath().substring(1) : url.getPath();
        } catch (Exception e) {
            throw new RuntimeException("S3 URL에서 키 추출 실패", e);
        }
    }

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

    protected String uploadScriptImage(final MultipartFile[] files, final String title, final UUID id) {
        try {
            if(files.length > 1)
                throw new RuntimeException("작품 이미지가 1개를 초과함");

            if(!Objects.equals(files[0].getContentType(), "image/jpeg") &&
                    !Objects.equals(files[0].getContentType(), "image/jpg") &&
                    !Objects.equals(files[0].getContentType(), "image/png"))
                throw new RuntimeException("ScriptImage file type is only jpg and png");

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
            final ProductEntity product = productRepo.findById(id);

            if(product == null)
                throw new RuntimeException("상품을 찾을 수 없습니다.");

            if(product.getImagePath() != null)
                deleteFile(bucket, product.getImagePath());

            // 저장
            try (InputStream inputStream = files[0].getInputStream()) {
                amazonS3.putObject(bucket, S3Key, inputStream, metadata);
            }

            return S3Key;
        } catch (Exception e) {
            throw new RuntimeException("스크립트 이미지 업로드 실패", e);
        }
    }

    protected String uploadDescription(final MultipartFile[] files, final String title, final UUID id) {
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
            final String S3Key = descriptionBucketFolder + fileName[0] + "\\" + title + "\\" + dateFormat.format(time) + ".pdf";

            final ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(files[0].getSize());
            metadata.setContentType("application/pdf");

            // 기존 파일 삭제
            final ProductEntity product = productRepo.findById(id);

            if(product == null)
                throw new RuntimeException("상품을 찾을 수 없습니다.");

            if(product.getDescriptionPath() != null)
                deleteFile(bucket, product.getDescriptionPath());

            // 저장
            try (InputStream inputStream = files[0].getInputStream()) {
                amazonS3.putObject(bucket, S3Key, inputStream, metadata);
            }

            return S3Key;
        } catch (Exception e) {
            throw new RuntimeException("설명 파일 업로드 실패", e);
        }
    }

    private static boolean isValidTitle(String title) {
        String regx_title = "^.{1,20}$";

        if(title == null) {
            log.warn("title is null or empty");
            return false;
        } else if(!Pattern.matches(regx_title, title)) {
            log.warn("title is not fit in the rule");
            return false;
        } else {
            log.info("title valid checked");
            return true;
        }
    }

    private static boolean isValidPlot(String plot) {
        String regx_plot = "^.{1,150}$";

        if(plot == null) {
            log.warn("plot is null or empty");
            return false;
        } else if(!Pattern.matches(regx_plot, plot)) {
            log.warn("plot is not fit in the rule");
            return false;
        } else {
            log.info("plot valid checked");
            return true;
        }
    }
}
