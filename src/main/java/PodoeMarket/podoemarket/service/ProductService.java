package PodoeMarket.podoemarket.service;

import PodoeMarket.podoemarket.Utils.EntityToDTOConverter;
import PodoeMarket.podoemarket.dto.ProductDTO;
import PodoeMarket.podoemarket.dto.response.ProductListDTO;
import PodoeMarket.podoemarket.entity.*;
import PodoeMarket.podoemarket.repository.OrderItemRepository;
import PodoeMarket.podoemarket.repository.ProductRepository;
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
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Slf4j
@Service
public class ProductService {
    private final ProductRepository productRepo;
    private final OrderItemRepository orderItemRepo;

    @Value("${cloud.aws.s3.url}")
    private String bucketURL;

    private final Pageable mainPage = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));

    public List<ProductListDTO> mainLongPlayList() {
        final List<ProductEntity> longPlays = productRepo.findAllByPlayTypeAndChecked(1, true, mainPage);

        return longPlays.stream()
                .filter(entity -> entity.getUser() != null)
                .map(entity -> EntityToDTOConverter.convertToProductList(entity, bucketURL))
                .collect(Collectors.toList());
    }

    public List<ProductListDTO> mainShortPlayList() {
        final List<ProductEntity> shortPlays = productRepo.findAllByPlayTypeAndChecked(2, true, mainPage);

        return shortPlays.stream()
                .filter(entity -> entity.getUser() != null)
                .map(entity -> EntityToDTOConverter.convertToProductList(entity, bucketURL))
                .collect(Collectors.toList());
    }

    public List<ProductListDTO> longPlayList(int page) {
        final Pageable pageable = PageRequest.of(page, 20, Sort.by(Sort.Direction.DESC, "createdAt"));

        final List<ProductEntity> longPlays = productRepo.findAllByPlayTypeAndChecked(1, true, pageable);

        return longPlays.stream()
                .filter(entity -> entity.getUser() != null)
                .map(entity -> EntityToDTOConverter.convertToProductList(entity, bucketURL))
                .collect(Collectors.toList());
    }

    public List<ProductListDTO> shortPlayList(int page) {
        final Pageable pageable = PageRequest.of(page, 20, Sort.by(Sort.Direction.DESC, "createdAt"));

        final List<ProductEntity> shortPlays = productRepo.findAllByPlayTypeAndChecked(2, true, pageable);

        return shortPlays.stream()
                .filter(entity -> entity.getUser() != null)
                .map(entity -> EntityToDTOConverter.convertToProductList(entity, bucketURL))
                .collect(Collectors.toList());
    }

    public ProductEntity product(UUID id) {
        return productRepo.findById(id);
    }

    public boolean isBuyScript(UUID userId, UUID productId) {
        final List<OrderItemEntity> orderItems = orderItemRepo.findByProductIdAndUserId(productId, userId);

        for(OrderItemEntity item : orderItems) {
            if(item.isScript())
                return true;
        }
        return false;
    }

    public ProductDTO productDetail(UUID productId, boolean isBuyScript) {
        final ProductEntity script = product(productId);

        return EntityToDTOConverter.convertToSingleProductDTO(script, isBuyScript, bucketURL);
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
}
