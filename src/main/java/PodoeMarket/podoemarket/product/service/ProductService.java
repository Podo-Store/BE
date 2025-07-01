package PodoeMarket.podoemarket.product.service;

import PodoeMarket.podoemarket.common.entity.*;
import PodoeMarket.podoemarket.common.repository.*;
import PodoeMarket.podoemarket.common.entity.type.PlayType;
import PodoeMarket.podoemarket.common.entity.type.ProductStatus;
import PodoeMarket.podoemarket.product.dto.request.ReviewRequestDTO;
import PodoeMarket.podoemarket.product.dto.response.ScriptDetailResponseDTO;
import PodoeMarket.podoemarket.product.dto.response.ScriptListResponseDTO;
import PodoeMarket.podoemarket.product.type.SortType;
import PodoeMarket.podoemarket.service.ViewCountService;
import com.itextpdf.io.source.ByteArrayOutputStream;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.ReaderProperties;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@Slf4j
@Service
public class ProductService {
    private final ProductRepository productRepo;
    private final OrderItemRepository orderItemRepo;
    private final ApplicantRepository applicantRepo;
    private final ProductLikeRepository productLikeRepo;
    private final ReviewRepository reviewRepo;

    private final ViewCountService viewCountService;

    @Value("${cloud.aws.s3.url}")
    private String bucketURL;

    public List<ScriptListResponseDTO.ProductListDTO> getPlayList(int page, UserEntity userInfo, PlayType playType, int pageSize, SortType sortType) {
        try {
            Sort sort = createSort(sortType);
            final Pageable pageable = PageRequest.of(page, pageSize, sort);
            final List<ProductEntity> plays = productRepo.findAllValidPlays(playType, ProductStatus.PASS, pageable);

            return plays.stream()
                    .map(play -> {
                        ScriptListResponseDTO.ProductListDTO productListDTO = new ScriptListResponseDTO.ProductListDTO();
                        String encodedScriptImage = play.getImagePath() != null ? bucketURL + URLEncoder.encode(play.getImagePath(), StandardCharsets.UTF_8) : "";

                        productListDTO.setId(play.getId());
                        productListDTO.setTitle(play.getTitle());
                        productListDTO.setWriter(play.getWriter());
                        productListDTO.setImagePath(encodedScriptImage);
                        productListDTO.setScript(play.getScript());
                        productListDTO.setScriptPrice(play.getScriptPrice());
                        productListDTO.setPerformance(play.getPerformance());
                        productListDTO.setPerformancePrice(play.getPerformancePrice());
                        productListDTO.setDate(play.getCreatedAt());
                        productListDTO.setChecked(play.getChecked());
                        productListDTO.setLike(getLikeStatus(userInfo, play.getId()));
                        productListDTO.setLikeCount(play.getLikeCount());
                        productListDTO.setViewCount(viewCountService.getProductViewCount(play.getId()));

                        return productListDTO;
                    }).toList();
        } catch (Exception e) {
            throw e;
        }
    }

    public ProductEntity getProduct(UUID id) {
        try {
            return productRepo.findById(id);
        } catch (Exception e) {
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public ScriptDetailResponseDTO getScriptDetailInfo(UserEntity userInfo, UUID productId) {
        try {
            // 조회수 증가
            viewCountService.incrementViewForProduct(productId);

            final ProductEntity script = productRepo.findById(productId);

            String imagePath = script.getImagePath() != null ? bucketURL + URLEncoder.encode(script.getImagePath(), StandardCharsets.UTF_8) : "";
            String descriptionPath = script.getDescriptionPath() != null ? bucketURL + URLEncoder.encode(script.getDescriptionPath(), StandardCharsets.UTF_8) : "";

            return ScriptDetailResponseDTO.builder()
                    .id(script.getId())
                    .title(script.getTitle())
                    .writer(script.getWriter())
                    .imagePath(imagePath)
                    .script(script.getScript())
                    .scriptPrice(script.getScriptPrice())
                    .performance(script.getPerformance())
                    .performancePrice(script.getPerformancePrice())
                    .descriptionPath(descriptionPath)
                    .date(script.getCreatedAt())
                    .checked(script.getChecked())
                    .playType(script.getPlayType())
                    .plot(script.getPlot())
                    .any(script.getAny())
                    .male(script.getMale())
                    .female(script.getFemale())
                    .stageComment(script.getStageComment())
                    .runningTime(script.getRunningTime())
                    .scene(script.getScene())
                    .act(script.getAct())
                    .buyStatus(buyStatus(userInfo, productId)) // 로그인한 유저의 해당 작품 구매 이력 확인
                    .like(getLikeStatus(userInfo, productId)) // 로그인한 유저의 좋아요 여부 확인
                    .likeCount(script.getLikeCount()) // 총 좋아요 수
                    .viewCount(viewCountService.getProductViewCount(productId)) // 총 조회수
                    .build();
        } catch (Exception e) {
            throw  e;
        }

    }

    // 트랜잭션 없는 PDF 처리 메서드
    public ResponseEntity<StreamingResponseBody> generateScriptPreview(String preSignedURL, int pagesToExtract) {
        // PDF 처리는 트랜잭션과 분리
        PdfExtractionResult result = processPreviewPdf(preSignedURL, pagesToExtract);

        StreamingResponseBody streamingResponseBody = outputStream -> {
            try (InputStream extractedPdfStream = new ByteArrayInputStream(result.getExtractedPdfBytes())) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = extractedPdfStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                    outputStream.flush();
                }
            } catch (Exception e) {
                throw new RuntimeException("PDF 스트리밍 중 오류 발생");
            }
        };

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header("X-Total-Pages", String.valueOf(result.getTotalPageCount()))
                .body(streamingResponseBody);
    }

    @Transactional
    public String toggleLike(UserEntity userInfo, UUID productId) {
        try {
            if (getLikeStatus(userInfo, productId)) {
                deleteLike(userInfo, productId);
                return "cancel like";
            } else {
                final ProductEntity product = productRepo.findById(productId);

                if(product == null)
                    throw new RuntimeException("상품을 찾을 수 업습니다.");

                final ProductLikeEntity like = ProductLikeEntity.builder()
                        .user(userInfo)
                        .product(product)
                        .build();
                createLike(like, productId);
                return "like";
            }
        } catch (Exception e) {
            throw e;
        }
    }

    public ResponseEntity<StreamingResponseBody> generateFullScriptDirect(String preSignedURL) {
        StreamingResponseBody streamingResponseBody = outputStream -> {
            try (InputStream inputStream = new URI(preSignedURL).toURL().openStream()) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                    outputStream.flush();
                }
            } catch (Exception e) {
                throw new RuntimeException("PDF 스트리밍 중 오류 발생");
            }
        };

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .body(streamingResponseBody);
    }

    public boolean getLikeStatus(final UserEntity userInfo, final UUID productId) {
        try {
            if (userInfo == null)
                return false;

            return productLikeRepo.existsByUserAndProductId(userInfo, productId);
        } catch (Exception e) {
            return false; // 오류 발생 시 좋아요하지 않은 것으로 처리
        }
    }

    @Transactional
    public void writeReview(final UserEntity userInfo, final ReviewRequestDTO dto) {
        try {
            // 평점이 존재하고, 범위가 1 ~ 5인지
            if (dto.getRating() == null || dto.getRating() < 1 || dto.getRating() > 5)
                throw new RuntimeException("평점이 올바르지 않습니다.");

            // 평가 기준이 존재하는가
            if (dto.getStandardType() == null)
                throw new RuntimeException("평가 기준을 선택해주세요.");

            // 내용이 50자 이상인지 확인
            if (dto.getContent().length() < 50)
                throw new RuntimeException("평가 내용을 50자 이상 작성해주세요.");

            final ProductEntity product = productRepo.findById(dto.getProductId());

            if(product == null)
                throw new RuntimeException("상품을 찾을 수 업습니다.");

            ReviewEntity review = ReviewEntity.builder()
                    .rating(dto.getRating())
                    .standardType(dto.getStandardType())
                    .content(dto.getContent())
                    .user(userInfo)
                    .product(product)
                    .build();

            reviewRepo.save(review);
        } catch (Exception e) {
            throw e;
        }
    }

    // ============== private (protected) method ===============
    private Sort createSort(SortType sortType) {
        return sortType.createSort();
    }

    // PDF 추출 결과를 저장할 클래스
    @Getter
    private static class PdfExtractionResult {
        private final int totalPageCount;
        private final byte[] extractedPdfBytes;

        public PdfExtractionResult(int totalPageCount, byte[] extractedPdfBytes) {
            this.totalPageCount = totalPageCount;
            this.extractedPdfBytes = extractedPdfBytes;
        }
    }

    // 트랜잭션과 분리된 PDF 처리 메서드
    private PdfExtractionResult processPreviewPdf(String preSignedURL, int pagesToExtract) {
        InputStream fileStream = null;
        try {
            fileStream = new URI(preSignedURL).toURL().openStream();
            return extractPagesFromPdf(fileStream, pagesToExtract);
        } catch (Exception e) {
            throw new RuntimeException("PDF 처리 실패", e);
        } finally {
            if (fileStream != null) {
                try {
                    fileStream.close();
                } catch (IOException e) {
                    log.error("InputStream 닫기 실패: {}", e.getMessage());
                }
            }
        }
    }

    // PDF의 특정 페이지까지 추출하는 함수
    private PdfExtractionResult extractPagesFromPdf(InputStream fileStream, int pagesToExtract) {
        PdfReader reader = null;
        PdfWriter writer = null;
        PdfDocument originalDoc = null;
        PdfDocument newDoc = null;
        ByteArrayOutputStream outputStream = null;

        try {
            outputStream = new ByteArrayOutputStream();

            // 안전 모드 설정 추가
            ReaderProperties properties = new ReaderProperties();
            reader = new PdfReader(fileStream, properties);
            reader.setMemorySavingMode(true); // 메모리 절약 모드 활성화

            writer = new PdfWriter(outputStream);

            originalDoc = new PdfDocument(reader);
            newDoc = new PdfDocument(writer);

            final int totalPageCount = originalDoc.getNumberOfPages();
            final int endPage = Math.min(pagesToExtract, totalPageCount);

            originalDoc.copyPagesTo(1, endPage, newDoc);

            // 명시적으로 문서 닫기 (역순으로)
            newDoc.close();
            originalDoc.close();
            writer.close();
            reader.close();

            return new PdfExtractionResult(totalPageCount, outputStream.toByteArray());
        } catch (Exception e) {
            closeAllResources(newDoc, originalDoc, writer, reader, outputStream);
            throw new RuntimeException("PDF 처리 실패", e);
        }
    }

    private void closeAllResources(PdfDocument newDoc, PdfDocument originalDoc, PdfWriter writer, PdfReader reader, ByteArrayOutputStream outputStream) {
        closeQuietly(newDoc, "newDoc");
        closeQuietly(originalDoc, "originalDoc");
        closeQuietly(writer, "writer");
        closeQuietly(reader, "reader");
        closeQuietly(outputStream, "outputStream");
    }

    // 리소스를 안전하게 닫는 유틸리티 메서드
    private void closeQuietly(AutoCloseable resource, String resourceName) {
        if (resource != null) {
            try {
                resource.close();
            } catch (Exception e) {
                log.error("{} 닫기 실패: {}", resourceName, e.getMessage());
            }
        }
    }

    @Transactional
    protected void deleteLike(final UserEntity userInfo, final UUID productId) {
        try {
            productLikeRepo.delete(productLikeRepo.findByUserAndProductId(userInfo, productId));
            productRepo.decrementLikeCount(productId);
        } catch (Exception e) {
            throw new RuntimeException("좋아요 삭제 실패", e);
        }
    }

    @Transactional
    protected void createLike(final ProductLikeEntity like, final UUID productId) {
        try {
            productLikeRepo.save(like);
            productRepo.incrementLikeCount(productId);
        } catch (Exception e) {
            throw new RuntimeException("좋아요 생성 실패", e);
        }
    }

    private int buyStatus(UserEntity userInfo, UUID productId) {
        try {
            if(userInfo == null)
                return 0;

            final List<OrderItemEntity> orderItems = orderItemRepo.findByProductIdAndUserId(productId, userInfo.getId());

            for(OrderItemEntity item : orderItems) {
                final boolean isBuyScript = item.getScript(); // 대본 구매 여부
                final boolean isExpiryDate = LocalDateTime.now().isAfter(item.getCreatedAt().plusYears(1)); // 권리 기간 만료 여부
                final boolean isBuyPerformance = applicantRepo.existsByOrderItemId(item.getId()); // 공연권 구매 여부

                if(isBuyScript && !isExpiryDate) { // 대본 구매 (대본 권리 기간 유효)
                    return 1;
                } else if(isBuyScript && !isExpiryDate && isBuyPerformance) { // 대본 + 공연권 구매 (대본 권리 기간 유효)
                    return 1;
                }
                else if(isBuyScript && isExpiryDate && isBuyPerformance) { // 공연권만 보유
                    return 2;
                }
            }

            return 0;
        } catch (Exception e) {
            return 0; // 오류 발생 시 구매하지 않은 것으로 처리
        }

    }
}
