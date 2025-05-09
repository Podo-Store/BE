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
import PodoeMarket.podoemarket.service.ViewCountService;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;
import org.springframework.transaction.annotation.Transactional;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Slf4j
@Service
public class ProductService {
    private final ProductRepository productRepo;
    private final OrderItemRepository orderItemRepo;
    private final ApplicantRepository applicantRepo;
    private final ProductLikeRepository productLikeRepo;
    private final ViewCountService viewCountService;

    @Value("${cloud.aws.s3.url}")
    private String bucketURL;

    public List<ScriptListResponseDTO.ProductListDTO> getPlayList(int page, UserEntity userInfo, PlayType playType, int pageSize) {
        final Pageable pageable = PageRequest.of(page, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        final List<ProductEntity> plays = productRepo.findAllByPlayTypeAndChecked(playType, ProductStatus.PASS, pageable);

        return plays.stream()
                .filter(play -> play.getUser() != null)
                .filter(play -> play.getScript() || play.getPerformance())
                .map(play -> convertToProductList(play, userInfo))
                .collect(Collectors.toList());
    }

    public ProductEntity product(UUID id) {
        return productRepo.findById(id);
    }

    public int buyStatus(UserEntity userInfo, UUID productId) {
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
    }

    public ScriptDetailResponseDTO productDetail(UUID productId, int buyStatus, boolean likeStatus, int likeCount, long viewCount) throws UnsupportedEncodingException {
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

        scriptDetailDTO.setAny(script.getAny());
        scriptDetailDTO.setMale(script.getMale());
        scriptDetailDTO.setFemale(script.getFemale());
        scriptDetailDTO.setStageComment(script.getStageComment());
        scriptDetailDTO.setRunningTime(script.getRunningTime());
        scriptDetailDTO.setScene(script.getScene());
        scriptDetailDTO.setAct(script.getAct());

        scriptDetailDTO.setBuyStatus(buyStatus);
        scriptDetailDTO.setLike(likeStatus);
        scriptDetailDTO.setLikeCount(likeCount);
        scriptDetailDTO.setViewCount(viewCount);

        return scriptDetailDTO;
    }

    // PDF의 특정 페이지까지 추출하는 함수
    public PdfExtractionResult extractPagesFromPdf(InputStream fileStream, int pagesToExtract) throws Exception {
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
        }
    }

    // PDF 추출 결과를 저장할 클래스
    @Getter
    public static class PdfExtractionResult {
        private final int totalPageCount;
        private final byte[] extractedPdfBytes;

        public PdfExtractionResult(int totalPageCount, byte[] extractedPdfBytes) {
            this.totalPageCount = totalPageCount;
            this.extractedPdfBytes = extractedPdfBytes;
        }
    }

    public int getLikeCount(final UUID productId) {
        return productLikeRepo.countByProductId(productId);
    }

    public boolean getLikeStatus(final UserEntity userInfo, final UUID productId) {
        return productLikeRepo.existsByUserAndProductId(userInfo, productId);
    }

    @Transactional
    public void deleteLike(final UserEntity userInfo, final UUID productId) {
        productLikeRepo.delete(productLikeRepo.findByUserAndProductId(userInfo, productId));
    }

    @Transactional
    public void createLike(final ProductLikeEntity like) {
        productLikeRepo.save(like);
    }

    // =========== private method ============
    private ScriptListResponseDTO.ProductListDTO convertToProductList(ProductEntity entity, UserEntity userInfo) {
        try {
            ScriptListResponseDTO.ProductListDTO productListDTO = new ScriptListResponseDTO.ProductListDTO();
            String encodedScriptImage = entity.getImagePath() != null ? bucketURL + URLEncoder.encode(entity.getImagePath(), "UTF-8") : "";

            productListDTO.setId(entity.getId());
            productListDTO.setTitle(entity.getTitle());
            productListDTO.setWriter(entity.getWriter());
            productListDTO.setImagePath(encodedScriptImage);
            productListDTO.setScript(entity.getScript());
            productListDTO.setScriptPrice(entity.getScriptPrice());
            productListDTO.setPerformance(entity.getPerformance());
            productListDTO.setPerformancePrice(entity.getPerformancePrice());
            productListDTO.setDate(entity.getCreatedAt());
            productListDTO.setChecked(entity.getChecked());
            productListDTO.setLike(getLikeStatus(userInfo, entity.getId()));
            productListDTO.setLikeCount(getLikeCount(entity.getId()));
            productListDTO.setViewCount(viewCountService.getProductViewCount(entity.getId()));

            return productListDTO;
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
}
