package PodoeMarket.podoemarket.product.service;

import PodoeMarket.podoemarket.common.entity.*;
import PodoeMarket.podoemarket.common.entity.type.StandardType;
import PodoeMarket.podoemarket.common.repository.*;
import PodoeMarket.podoemarket.common.entity.type.PlayType;
import PodoeMarket.podoemarket.common.entity.type.ProductStatus;
import PodoeMarket.podoemarket.product.dto.request.ReviewRequestDTO;
import PodoeMarket.podoemarket.product.dto.response.ScriptDetailResponseDTO;
import PodoeMarket.podoemarket.product.dto.response.ScriptListResponseDTO;
import PodoeMarket.podoemarket.product.type.ProductSortType;
import PodoeMarket.podoemarket.product.type.ReviewSortType;
import PodoeMarket.podoemarket.service.S3Service;
import PodoeMarket.podoemarket.service.ViewCountService;
import com.itextpdf.io.source.ByteArrayOutputStream;
import org.apache.pdfbox.Loader;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@RequiredArgsConstructor
@Slf4j
@Service
public class ProductService {
    private final ProductRepository productRepo;
    private final OrderItemRepository orderItemRepo;
    private final ApplicantRepository applicantRepo;
    private final ProductLikeRepository productLikeRepo;
    private final ReviewRepository reviewRepo;
    private final ReviewLikeRepository reviewLikeRepo;

    private final ViewCountService viewCountService;
    private final S3Service s3Service;

    public List<ScriptListResponseDTO.ProductListDTO> getPlayList(int page, UserEntity userInfo, PlayType playType, int pageSize, ProductSortType sortType) {
        try {
            Sort sort = createProductSort(sortType);
            final Pageable pageable = PageRequest.of(page, pageSize, sort);
            final List<ProductEntity> plays = productRepo.findAllValidPlays(playType, ProductStatus.PASS, pageable);

            return plays.stream()
                    .map(play -> {
                        final ScriptListResponseDTO.ProductListDTO productListDTO = new ScriptListResponseDTO.ProductListDTO();

                        final String scriptImage = generateScriptImgURL(play);

                        productListDTO.setId(play.getId());
                        productListDTO.setTitle(play.getTitle());
                        productListDTO.setWriter(play.getWriter());
                        productListDTO.setImagePath(scriptImage);
                        productListDTO.setScript(play.getScript());
                        productListDTO.setScriptPrice(play.getScriptPrice());
                        productListDTO.setPerformance(play.getPerformance());
                        productListDTO.setPerformancePrice(play.getPerformancePrice());
                        productListDTO.setDate(play.getCreatedAt());
                        productListDTO.setChecked(play.getChecked());
                        productListDTO.setLike(getProductLikeStatus(userInfo, play.getId()));
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
    public ScriptDetailResponseDTO getScriptDetailInfo(UserEntity userInfo, UUID productId, int page, int pageSize, ReviewSortType sortType) {
        try {
            // 조회수 증가
            viewCountService.incrementViewForProduct(productId);

            final ProductEntity script = productRepo.findById(productId);
            final String scriptImage = generateScriptImgURL(script);

            ScriptDetailResponseDTO.ReviewStatisticsDTO reviewStatistics = getReviewStatistics(productId);
            List<ScriptDetailResponseDTO.ReviewListResponseDTO> reviewList = getReviewList(userInfo, productId, page, pageSize, sortType);

            return ScriptDetailResponseDTO.builder()
                    .id(script.getId())
                    .title(script.getTitle())
                    .writer(script.getWriter())
                    .imagePath(scriptImage)
                    .script(script.getScript())
                    .scriptPrice(script.getScriptPrice())
                    .performance(script.getPerformance())
                    .performancePrice(script.getPerformancePrice())
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
                    .like(getProductLikeStatus(userInfo, productId)) // 로그인한 유저의 좋아요 여부 확인
                    .likeCount(script.getLikeCount()) // 총 좋아요 수
                    .viewCount(viewCountService.getProductViewCount(productId)) // 총 조회수
                    .reviewStatistics(reviewStatistics)
                    .reviews(reviewList)
                    .build();
        } catch (Exception e) {
            throw  e;
        }
    }

    // 트랜잭션 없는 PDF 처리 메서드
    public ResponseEntity<StreamingResponseBody> generateScriptPreview(String preSignedURL, int pagesToExtract) throws IOException {
        // PDF 처리는 트랜잭션과 분리
        PdfExtractionResult result = extractPagesFromPdf(extractPdfFromZip(preSignedURL), pagesToExtract);

        StreamingResponseBody stream = outputStream -> {
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
                .body(stream);
    }

    @Transactional
    public String toggleLike(UserEntity userInfo, UUID productId) {
        try {
            if (getProductLikeStatus(userInfo, productId)) {
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

    public ResponseEntity<StreamingResponseBody> generateFullPDF(String preSignedURL) {
        StreamingResponseBody stream = outputStream -> {
            try {
                streamPdfFromZip(preSignedURL, outputStream);
            } catch (Exception e) {
                throw new RuntimeException("ZIP에서 PDF 추출 또는 스트리밍 중 오류 발생", e);
            }
        };

        return ResponseEntity.ok()
                .header("Content-Disposition", "inline; filename=\"script.pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(stream);
    }

    public boolean getProductLikeStatus(final UserEntity userInfo, final UUID productId) {
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

    @Transactional
    public void deleteReview(final UserEntity userInfo, final UUID id) {
        try {
            final ReviewEntity review = reviewRepo.findById(id);

            if (!review.getUser().getId().equals(userInfo.getId()))
                throw new RuntimeException("후기 작성자가 아닙니다.");

            reviewRepo.delete(review);
        } catch (Exception e) {
            throw e;
        }
    }

    // ============== private (protected) method ===============
    private Sort createProductSort(ProductSortType productSortType) {
        return productSortType.createSort();
    }

    private Sort createReviewSort(ReviewSortType reviewSortType) {
        return reviewSortType.createSort();
    }

    private String generateScriptImgURL(ProductEntity product) {
        return product.getImagePath() != null ? s3Service.generatePreSignedURL(product.getImagePath()) : "";
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

    // PDF 추출 메서드
    private static byte[] extractPdfFromZip(String preSignedURL) throws IOException {
        try (InputStream inputStream = new URI(preSignedURL).toURL().openStream();
             ZipInputStream zipInputStream = new ZipInputStream(inputStream);
             ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
            ZipEntry entry;

            while ((entry = zipInputStream.getNextEntry()) != null) {
                if (entry.getName().toLowerCase().endsWith(".pdf")) {
                    byte[] buffer = new byte[8192];
                    int len;

                    while ((len = zipInputStream.read(buffer)) > 0) {
                        byteArrayOutputStream.write(buffer, 0, len);
                    }

                    return byteArrayOutputStream.toByteArray();
                }
            }

            throw new RuntimeException("ZIP 파일 내에 PDF가 없습니다.");
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    // PDF 추출 메서드 (스트리밍 방식)
    private static void streamPdfFromZip(String preSignedURL, OutputStream outputStream) throws IOException {
        try (InputStream inputStream = new URI(preSignedURL).toURL().openStream();
             ZipInputStream zipInputStream = new ZipInputStream(inputStream)) {
            ZipEntry entry;
            boolean found = false;

            while ((entry = zipInputStream.getNextEntry()) != null) {
                if (entry.getName().toLowerCase().endsWith(".pdf")) {
                    found = true;
                    byte[] buffer = new byte[8192]; // 8KB 씩 스트리밍
                    int len;

                    while ((len = zipInputStream.read(buffer)) > 0) {
                        outputStream.write(buffer, 0, len);
                    }

                    outputStream.flush();
                    break;
                }
            }

            if (!found)
                throw new RuntimeException("ZIP 파일 내에 PDF가 없습니다.");

        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    // PDF의 특정 페이지까지 추출하는 함수
    private PdfExtractionResult extractPagesFromPdf(byte[] pdfBytes, int pagesToExtract) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            int totalPages = document.getNumberOfPages();
            int endPage = Math.min(pagesToExtract, totalPages);

            try (PDDocument newDoc = new PDDocument()) {
                for (int i = 0; i < endPage; i++) {
                    PDPage page = document.getPage(i);
                    newDoc.addPage(page);
                }
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                newDoc.save(baos);

                return new PdfExtractionResult(totalPages, baos.toByteArray());
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

    private int buyStatus(final UserEntity userInfo, final UUID productId) {
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

    private boolean getReviewLikeStatus(final UserEntity userInfo, final UUID reviewId) {
        try {
            if (userInfo == null)
                return false;

            return reviewLikeRepo.existsByUserAndReviewId(userInfo, reviewId);
        } catch (Exception e) {
            return false;
        }
    }

    private List<ScriptDetailResponseDTO.ReviewListResponseDTO> getReviewList(final UserEntity userInfo, final UUID productId,
                                                                              final int page, final int pageSize, final ReviewSortType sortType) {
        Sort sort = createReviewSort(sortType);
        final Pageable pageable = PageRequest.of(page, pageSize, sort);

        return reviewRepo.findAllByProductId(productId, pageable).stream()
                .map(review -> ScriptDetailResponseDTO.ReviewListResponseDTO.builder()
                        .id(review.getId())
                        .nickname(review.getUser().getNickname())
                        .date(review.getCreatedAt())
                        .myself(review.getUser().getId().equals(userInfo.getId()))
                        .rating(review.getRating())
                        .standardType(review.getStandardType())
                        .content(review.getContent())
                        .isLike(getReviewLikeStatus(userInfo, review.getId()))
                        .likeCount(review.getLikeCount())
                        .build()
                ).toList();
    }

    private ScriptDetailResponseDTO.ReviewStatisticsDTO getReviewStatistics(final UUID productId) {
        final Integer totalCount = reviewRepo.countByProductId(productId);

        final Integer fiveStarCount = reviewRepo.countByProductIdAndRating(productId, 5);
        final Integer fourStarCount = reviewRepo.countByProductIdAndRating(productId, 4);
        final Integer threeStarCount = reviewRepo.countByProductIdAndRating(productId, 3);
        final Integer twoStarCount = reviewRepo.countByProductIdAndRating(productId, 2);
        final Integer oneStarCount = reviewRepo.countByProductIdAndRating(productId, 1);

        final Integer characterCount = reviewRepo.countByProductIdAndStandardType(productId, StandardType.CHARACTER);
        final Integer relationCount = reviewRepo.countByProductIdAndStandardType(productId, StandardType.RELATION);
        final Integer storyCount = reviewRepo.countByProductIdAndStandardType(productId, StandardType.STORY);

        final double reviewAverageRating = (double) (5 * fiveStarCount + 4 * fourStarCount + 3 * threeStarCount + 2 * twoStarCount + oneStarCount) / totalCount;

        return ScriptDetailResponseDTO.ReviewStatisticsDTO.builder()
                .totalReviewCount(totalCount)
                .reviewAverageRating(rounding(reviewAverageRating))
                .fiveStarPercent(percentage(fiveStarCount, totalCount))
                .fourStarPercent(percentage(fourStarCount, totalCount))
                .threeStarPercent(percentage(threeStarCount, totalCount))
                .twoStarPercent(percentage(twoStarCount, totalCount))
                .oneStarPercent(percentage(oneStarCount, totalCount))
                .characterPercent(percentage(characterCount, totalCount))
                .relationPercent(percentage(relationCount, totalCount))
                .storyPercent(percentage(storyCount, totalCount))
                .build();
    }

    private double rounding(double value) {
        return (double) Math.round(value * 100.0) / 100.0;
    }

    private double percentage(final int reviewCnt, final int totalCnt) {
        double ratio = ((double) reviewCnt / totalCnt) * 100.0;

        return rounding(ratio);
    }
}
