package PodoeMarket.podoemarket.product.service;

import PodoeMarket.podoemarket.common.entity.*;
import PodoeMarket.podoemarket.common.entity.type.*;
import PodoeMarket.podoemarket.common.repository.*;
import PodoeMarket.podoemarket.product.dto.request.ReviewRequestDTO;
import PodoeMarket.podoemarket.product.dto.request.ReviewUpdateRequestDTO;
import PodoeMarket.podoemarket.product.dto.response.ReviewResponseDTO;
import PodoeMarket.podoemarket.product.dto.response.ScriptDetailResponseDTO;
import PodoeMarket.podoemarket.product.dto.response.ScriptListResponseDTO;
import PodoeMarket.podoemarket.product.type.ProductSortType;
import PodoeMarket.podoemarket.product.type.ReviewSortType;
import PodoeMarket.podoemarket.service.S3Service;
import PodoeMarket.podoemarket.service.ViewCountService;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.S3Object;
import com.itextpdf.io.source.ByteArrayOutputStream;
import org.apache.pdfbox.Loader;
import org.springframework.beans.factory.annotation.Value;
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
import java.net.URL;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
    private final ProductLikeRepository productLikeRepo;
    private final ReviewRepository reviewRepo;
    private final ReviewLikeRepository reviewLikeRepo;

    private final ViewCountService viewCountService;
    private final S3Service s3Service;
    private final AmazonS3 amazonS3;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    public List<ScriptListResponseDTO.ProductListDTO> getPlayList(int page, UserEntity userInfo, PlayType playType, int pageSize, ProductSortType sortType) {
        try {
            List<ProductStatus> validStatuses = List.of(ProductStatus.PASS, ProductStatus.RE_WAIT, ProductStatus.RE_PASS);

            Sort sort = createProductSort(sortType);
            List<ProductEntity> plays = productRepo.findAllValidPlays(
                    playType,
                    validStatuses,
                    PageRequest.of(page, pageSize, sort)
            );

            return plays.stream()
                    .map(play -> getListDTO(userInfo, play))
                    .toList();
        } catch (Exception e) {
            throw new RuntimeException("작품 목록 조회 실패", e);
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
            boolean isReviewWritten = false;
            boolean isMine = false;

            if (userInfo != null) {
                if (script.getUser().getId().equals(userInfo.getId()))
                    isMine = true;

                final ReviewEntity review = reviewRepo.findByProductAndUserId(script, userInfo.getId());

                if (review != null)
                    isReviewWritten = true;
            }

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
                    .intention(script.getIntention())
                    .buyOptions(buyOption(userInfo, productId)) // 로그인한 유저의 해당 작품 구매 이력 확인
                    .like(getProductLikeStatus(userInfo, productId)) // 로그인한 유저의 좋아요 여부 확인
                    .likeCount(script.getLikeCount()) // 총 좋아요 수
                    .isReviewWritten(isReviewWritten)
                    .viewCount(viewCountService.getProductViewCount(productId)) // 총 조회수
                    .isMine(isMine)
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
    public String toggleLikeProduct(UserEntity userInfo, UUID productId) {
        try {
            if (getProductLikeStatus(userInfo, productId)) {
                deleteProductLike(userInfo, productId);

                return "cancel like";
            } else {
                final ProductEntity product = productRepo.findById(productId);

                if(product == null)
                    throw new RuntimeException("상품을 찾을 수 업습니다.");

                final ProductLikeEntity like = ProductLikeEntity.builder()
                        .user(userInfo)
                        .product(product)
                        .build();

                createProductLike(like, productId);

                return "like";
            }
        } catch (Exception e) {
            throw e;
        }
    }

    public StreamingResponseBody viewScript(final UUID productId) {
        return outputStream -> {
            ProductEntity product = getProduct(productId);

            try(S3Object s3Object = amazonS3.getObject(bucket, product.getFilePath());
                 InputStream s3Stream = s3Object.getObjectContent()) {

                streamPdfFromZip(s3Stream, outputStream);
            }
        };
    }

    public StreamingResponseBody viewDescription(final UUID productId) {
        return outputStream -> {
            ProductEntity product = getProduct(productId);

            try(S3Object s3Object = amazonS3.getObject(bucket, product.getDescriptionPath());
                InputStream s3Stream = s3Object.getObjectContent()) {

                streamPdfFromZip(s3Stream, outputStream);
            }
        };
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

    public ReviewResponseDTO getWriteReview(final UserEntity userInfo, final UUID productId) {
        try {
            if(userInfo == null)
                throw new RuntimeException("로그인이 필요한 서비스입니다.");

            final ProductEntity product = productRepo.findById(productId);

            if(product == null)
                throw new RuntimeException("상품을 찾을 수 없습니다.");

            final ReviewEntity review = reviewRepo.findByProductAndUserId(product, userInfo.getId());

            final String scriptImage = generateScriptImgURL(product);

            return ReviewResponseDTO.builder()
                    .id(review != null ? review.getId() : null)
                    .imagePath(scriptImage)
                    .title(product.getTitle())
                    .writer(product.getWriter())
                    .rating(review != null ? review.getRating() : null)
                    .standardType(review != null ? review.getStandardType() : null)
                    .content(review != null ? review.getContent() : null)
                    .build();
        } catch (Exception e) {
            throw e;
        }
    }

    @Transactional
    public void writeReview(final UserEntity userInfo, final ReviewRequestDTO dto) {
        try {
            // 평점이 존재하고, 범위가 1 ~ 5인지
            if(dto.getRating() == null || dto.getRating() < 1 || dto.getRating() > 5)
                throw new RuntimeException("평점이 올바르지 않습니다.");

            // 평가 기준이 존재하는가
            if(dto.getStandardType() == null)
                throw new RuntimeException("평가 기준을 선택해주세요.");

            // 내용이 50자 이상인지 확인
            if(dto.getContent().length() < 50)
                throw new RuntimeException("평가 내용을 50자 이상 작성해주세요.");

            final ProductEntity product = productRepo.findById(dto.getProductId());

            if(product == null)
                throw new RuntimeException("상품을 찾을 수 없습니다.");

            if(userInfo.getId().equals(product.getUser().getId()))
                throw new RuntimeException("본인 작품의 후기는 작성할 수 없습니다.");

            if(reviewRepo.existsByProductAndUserId(product, userInfo.getId()))
                throw new RuntimeException("후기는 하나만 작성 가능합니다.");

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

    @Transactional
    public void updateReview(final UserEntity userInfo, final UUID id, final ReviewUpdateRequestDTO dto) {
        try {
            ReviewEntity review = reviewRepo.findById(id);

            if (review == null)
                throw new RuntimeException("후기가 존재하지 않습니다.");

            if (!review.getUser().getId().equals(userInfo.getId()))
                throw new RuntimeException("후기 작성자가 아닙니다.");

            if (dto.getRating() != null)
                review.setRating(dto.getRating());

            if (dto.getStandardType() != null)
                review.setStandardType(dto.getStandardType());

            if (dto.getContent() != null)
                review.setContent(dto.getContent());

            reviewRepo.save(review);
        } catch (Exception e) {
            throw e;
        }
    }

    @Transactional
    public String toggleLikeReview(UserEntity userInfo, UUID reviewId) {
        try {
            if (getReviewLikeStatus(userInfo, reviewId)) {
                deleteReviewLike(userInfo, reviewId);

                return "cancel like";
            } else {
                final ReviewEntity review = reviewRepo.findById(reviewId);

                if(review == null)
                    throw new RuntimeException("후기를 찾을 수 업습니다.");

                final ReviewLikeEntity like = ReviewLikeEntity.builder()
                        .user(userInfo)
                        .review(review)
                        .build();

                createReviewLike(like, reviewId);

                return "like";
            }
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
        try (InputStream inputStream = new URL(preSignedURL).openStream();
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
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ZIP → PDF 추출 (스트리밍)
    private static void streamPdfFromZip(InputStream zipStream, OutputStream outputStream) throws IOException {
        try (ZipInputStream zipInputStream = new ZipInputStream(zipStream)) {
            ZipEntry entry;
            boolean found = false;

            byte[] buffer = new byte[8192];

            while ((entry = zipInputStream.getNextEntry()) != null) {

                if (entry.getName().toLowerCase().endsWith(".pdf")) {
                    found = true;
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

        } catch (Exception e) {
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
    protected void deleteProductLike(final UserEntity userInfo, final UUID productId) {
        try {
            productLikeRepo.delete(productLikeRepo.findByUserAndProductId(userInfo, productId));
            productRepo.decrementLikeCount(productId);
        } catch (Exception e) {
            throw new RuntimeException("좋아요 삭제 실패", e);
        }
    }

    @Transactional
    protected void createProductLike(final ProductLikeEntity like, final UUID productId) {
        try {
            productLikeRepo.save(like);
            productRepo.incrementLikeCount(productId);
        } catch (Exception e) {
            throw new RuntimeException("좋아요 생성 실패", e);
        }
    }

    private List<BuyOption> buyOption(final UserEntity userInfo, final UUID productId) {
        try {
            // <대본>
            // 권리기간(열람기간) : 3개월
            // 환불 : 불가
            // 한 번에 1개만 소유 가능

            List<BuyOption> options = new ArrayList<>();

            if (userInfo == null) {
                options.add(BuyOption.SCRIPT);
                options.add(BuyOption.PERFORMANCE);

                return options;
            }

            boolean hasValidScript = orderItemRepo.existsByProduct_IdAndUser_IdAndScriptTrueAndOrder_OrderStatusAndCreatedAtAfter(
                    productId, userInfo.getId(), OrderStatus.PAID, LocalDateTime.now().minusMonths(3)
            );

            // 유효한 대본이 없으면 대본 구매 가능
            if(!hasValidScript)
                options.add(BuyOption.SCRIPT);

            // 공연권은 항상 가능
            options.add(BuyOption.PERFORMANCE);

            return options;
        } catch (Exception e) {
            throw e;
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
                .map(review -> {
                    boolean isMyself = userInfo != null && userInfo.getId().equals(review.getUser().getId());
                    StageType stageType = review.getUser().getStageType() != null ? review.getUser().getStageType() : null;
                    boolean isEdited = !review.getCreatedAt().equals(review.getUpdatedAt());

                    return ScriptDetailResponseDTO.ReviewListResponseDTO.builder()
                            .id(review.getId())
                            .nickname(review.getUser().getNickname())
                            .stageType(stageType)
                            .date(!isEdited ? review.getCreatedAt() : review.getUpdatedAt())
                            .isEdited(isEdited)
                            .myself(isMyself)
                            .rating(review.getRating())
                            .standardType(review.getStandardType())
                            .content(review.getContent())
                            .isLike(getReviewLikeStatus(userInfo, review.getId()))
                            .likeCount(review.getLikeCount())
                            .build();
                }).toList();
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
                .totalReviewPages((int) Math.ceil((double) totalCount / 5)) // 5개씩 끊어서 전달
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

    @Transactional
    protected void deleteReviewLike(final UserEntity userInfo, final UUID reviewId) {
        try {
            reviewLikeRepo.delete(reviewLikeRepo.findByUserAndReviewId(userInfo, reviewId));
            reviewRepo.decrementLikeCount(reviewId);
        } catch (Exception e) {
            throw new RuntimeException("좋아요 삭제 실패", e);
        }
    }

    @Transactional
    protected void createReviewLike(final ReviewLikeEntity like, final UUID reviewId) {
        try {
            reviewLikeRepo.save(like);
            reviewRepo.incrementLikeCount(reviewId);
        } catch (Exception e) {
            throw new RuntimeException("좋아요 생성 실패", e);
        }
    }

    private ScriptListResponseDTO.ProductListDTO getListDTO(final UserEntity userInfo, final ProductEntity play) {
        ScriptListResponseDTO.ProductListDTO productListDTO = new ScriptListResponseDTO.ProductListDTO();

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
    }
}
