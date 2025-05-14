package PodoeMarket.podoemarket.product.service;

import PodoeMarket.podoemarket.common.entity.OrderItemEntity;
import PodoeMarket.podoemarket.common.entity.ProductEntity;
import PodoeMarket.podoemarket.common.entity.ProductLikeEntity;
import PodoeMarket.podoemarket.common.entity.UserEntity;
import PodoeMarket.podoemarket.common.repository.ProductLikeRepository;
import PodoeMarket.podoemarket.common.entity.type.PlayType;
import PodoeMarket.podoemarket.common.entity.type.ProductStatus;
import PodoeMarket.podoemarket.common.repository.ApplicantRepository;
import PodoeMarket.podoemarket.common.repository.OrderItemRepository;
import PodoeMarket.podoemarket.common.repository.ProductRepository;
import PodoeMarket.podoemarket.product.dto.response.ScriptDetailResponseDTO;
import PodoeMarket.podoemarket.product.dto.response.ScriptListResponseDTO;
import PodoeMarket.podoemarket.service.S3Service;
import PodoeMarket.podoemarket.service.ViewCountService;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;
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
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@Slf4j
@Service
@Transactional(readOnly = true)
public class ProductService {
    private final ProductRepository productRepo;
    private final OrderItemRepository orderItemRepo;
    private final ApplicantRepository applicantRepo;
    private final ProductLikeRepository productLikeRepo;
    private final ViewCountService viewCountService;
    private final S3Service s3Service;

    @Value("${cloud.aws.s3.url}")
    private String bucketURL;

    public List<ScriptListResponseDTO.ProductListDTO> getPlayList(int page, UserEntity userInfo, PlayType playType, int pageSize) {
        try {
            final Pageable pageable = PageRequest.of(page, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));
            final List<ProductEntity> plays = productRepo.findAllByPlayTypeAndChecked(playType, ProductStatus.PASS, pageable);

            return plays.stream()
                    .filter(play -> play.getUser() != null)
                    .filter(play -> play.getScript() || play.getPerformance())
                    .map(play -> {
                        ScriptListResponseDTO.ProductListDTO productListDTO = new ScriptListResponseDTO.ProductListDTO();
                        String encodedScriptImage = null;
                        try {
                            encodedScriptImage = play.getImagePath() != null ? bucketURL + URLEncoder.encode(play.getImagePath(), "UTF-8") : "";
                        } catch (UnsupportedEncodingException e) {
                            throw new RuntimeException(e);
                        }

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
                        productListDTO.setLikeCount(getLikeCount(play.getId()));
                        productListDTO.setViewCount(viewCountService.getProductViewCount(play.getId()));

                        return productListDTO;
                    }).toList();
        } catch (Exception e) {
            throw new RuntimeException("상품 목록 조회 실패", e);
        }
    }

    public ProductEntity getProduct(UUID id) {
        try {
            return productRepo.findById(id);
        } catch (Exception e) {
            throw new RuntimeException("상품 조회 실패", e);
        }
    }

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
                    .likeCount(getLikeCount(productId)) // 총 좋아요 수
                    .viewCount(viewCountService.getProductViewCount(productId)) // 총 조회수
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("상품 상세 정보 조회 실패", e);
        }

    }

    public ResponseEntity<StreamingResponseBody> generateScriptPreview(UUID productId) throws Exception {
        try {
            final ProductEntity product = getProduct(productId);
            final String s3Key = product.getFilePath();
            final String preSignedURL = s3Service.generatePreSignedURL(s3Key);

            int pagesToExtract = (product.getPlayType() == PlayType.LONG) ? 3 : 1;

            try (InputStream fileStream = (new URI(preSignedURL)).toURL().openStream()) {
                PdfExtractionResult result = extractPagesFromPdf(fileStream, pagesToExtract);

                StreamingResponseBody streamingResponseBody = outputStream -> {
                    try (InputStream extractedPdfStream = new ByteArrayInputStream(result.getExtractedPdfBytes())) {
                        byte[] buffer = new byte[8192];
                        int bytesRead;
                        while ((bytesRead = extractedPdfStream.read(buffer)) != -1) {
                            outputStream.write(buffer, 0, bytesRead);
                            outputStream.flush();
                        }
                    } catch (Exception e) {
                        log.error("PDF 스트리밍 중 오류 발생: {}", e.getMessage());
                    }
                };

                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_PDF)
                        .header("X-Total-Pages", String.valueOf(result.getTotalPageCount()))
                        .body(streamingResponseBody);
            }
        } catch (Exception e) {
            throw new RuntimeException("스크립트 미리보기 생성 실패", e);
        }
    }

    @Transactional
    public String toggleLike(UserEntity userInfo, UUID productId) {
        try {
            if (getLikeStatus(userInfo, productId)) {
                deleteLike(userInfo, productId);
                return "cancel like";
            } else {
                final ProductEntity product = getProduct(productId);
                final ProductLikeEntity like = ProductLikeEntity.builder()
                        .user(userInfo)
                        .product(product)
                        .build();
                createLike(like);
                return "like";
            }
        } catch (Exception e) {
            throw new RuntimeException("좋아요 처리 실패", e);
        }
    }

    // ============== private (protected) method ===============
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

    // PDF의 특정 페이지까지 추출하는 함수
    private PdfExtractionResult extractPagesFromPdf(InputStream fileStream, int pagesToExtract) throws Exception {
        try (PdfReader reader = new PdfReader(fileStream);
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            final PdfDocument originalDoc = new PdfDocument(reader);
            final int totalPageCount = originalDoc.getNumberOfPages();
            final PdfWriter writer = new PdfWriter(outputStream);
            final PdfDocument newDoc = new PdfDocument(writer);

            final int endPage = Math.min(pagesToExtract, totalPageCount);
            originalDoc.copyPagesTo(1, endPage, newDoc);
            newDoc.close();

            return new PdfExtractionResult(totalPageCount, outputStream.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException("PDF 페이지 추출 실패", e);
        }
    }

    @Transactional
    protected void deleteLike(final UserEntity userInfo, final UUID productId) {
        try {
            productLikeRepo.delete(productLikeRepo.findByUserAndProductId(userInfo, productId));
        } catch (Exception e) {
            throw new RuntimeException("좋아요 삭제 실패", e);
        }
    }

    @Transactional
    protected void createLike(final ProductLikeEntity like) {
        try {
            productLikeRepo.save(like);
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

    private int getLikeCount(final UUID productId) {
        try {
            return productLikeRepo.countByProductId(productId);
        } catch (Exception e) {
            return 0; // 오류 발생 시 0으로 처리
        }
    }

    private boolean getLikeStatus(final UserEntity userInfo, final UUID productId) {
        try {
            if (userInfo == null)
                return false;

            return productLikeRepo.existsByUserAndProductId(userInfo, productId);
        } catch (Exception e) {
            log.error("좋아요 상태 확인 중 오류 발생: userId={}, productId={}, error={}",
                    userInfo != null ? userInfo.getId() : "null", productId, e.getMessage());
            return false; // 오류 발생 시 좋아요하지 않은 것으로 처리
        }
    }
}
