package PodoeMarket.podoemarket.product.service;

import PodoeMarket.podoemarket.Utils.EntityToDTOConverter;
import PodoeMarket.podoemarket.common.entity.OrderItemEntity;
import PodoeMarket.podoemarket.common.entity.ProductEntity;
import PodoeMarket.podoemarket.common.entity.ProductLikeEntity;
import PodoeMarket.podoemarket.common.entity.UserEntity;
import PodoeMarket.podoemarket.common.repository.ProductLikeRepository;
import PodoeMarket.podoemarket.dto.ProductDTO;
import PodoeMarket.podoemarket.dto.response.ProductListDTO;
import PodoeMarket.podoemarket.common.entity.type.PlayType;
import PodoeMarket.podoemarket.common.entity.type.ProductStatus;
import PodoeMarket.podoemarket.common.repository.ApplicantRepository;
import PodoeMarket.podoemarket.common.repository.OrderItemRepository;
import PodoeMarket.podoemarket.common.repository.ProductRepository;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;
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

    @Value("${cloud.aws.s3.url}")
    private String bucketURL;

    private final Pageable mainPage = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));

    public List<ProductListDTO> mainLongPlayList() {
        final List<ProductEntity> longPlays = productRepo.findAllByPlayTypeAndChecked(PlayType.LONG, ProductStatus.PASS, mainPage);

        return longPlays.stream()
                .filter(play -> play.getUser() != null)
                .filter(play -> play.isScript() || play.isPerformance())
                .map(play -> EntityToDTOConverter.convertToProductList(play, bucketURL))
                .collect(Collectors.toList());
    }

    public List<ProductListDTO> mainShortPlayList() {
        final List<ProductEntity> shortPlays = productRepo.findAllByPlayTypeAndChecked(PlayType.SHORT,  ProductStatus.PASS, mainPage);

        return shortPlays.stream()
                .filter(play -> play.getUser() != null)
                .filter(play -> play.isScript() || play.isPerformance())
                .map(play -> EntityToDTOConverter.convertToProductList(play, bucketURL))
                .collect(Collectors.toList());
    }

    public List<ProductListDTO> longPlayList(int page) {
        final Pageable pageable = PageRequest.of(page, 20, Sort.by(Sort.Direction.DESC, "createdAt"));

        final List<ProductEntity> longPlays = productRepo.findAllByPlayTypeAndChecked(PlayType.LONG,  ProductStatus.PASS, pageable);

        return longPlays.stream()
                .filter(play -> play.getUser() != null)
                .filter(play -> play.isScript() || play.isPerformance())
                .map(play -> EntityToDTOConverter.convertToProductList(play, bucketURL))
                .collect(Collectors.toList());
    }

    public List<ProductListDTO> shortPlayList(int page) {
        final Pageable pageable = PageRequest.of(page, 20, Sort.by(Sort.Direction.DESC, "createdAt"));

        final List<ProductEntity> shortPlays = productRepo.findAllByPlayTypeAndChecked(PlayType.SHORT,  ProductStatus.PASS, pageable);

        return shortPlays.stream()
                .filter(play -> play.getUser() != null)
                .filter(play -> play.isScript() || play.isPerformance())
                .map(play -> EntityToDTOConverter.convertToProductList(play, bucketURL))
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
            final boolean isBuyScript = item.isScript(); // 대본 구매 여부
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

    public ProductDTO productDetail(UUID productId, int buyStatus) {
        final ProductEntity script = product(productId);

        return EntityToDTOConverter.convertToSingleProductDTO(script, buyStatus, bucketURL);
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

    public boolean likeStatus(UserEntity userInfo, UUID productId) {
        return productLikeRepo.existsByUserAndProductId(userInfo, productId);
    }

    public void deleteLike(UserEntity userInfo, UUID productId) {
        productLikeRepo.delete(productLikeRepo.findByUserAndProductId(userInfo, productId));
    }

    public void createLike(ProductLikeEntity like) {
        productLikeRepo.save(like);
    }
}
