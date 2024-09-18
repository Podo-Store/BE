package PodoeMarket.podoemarket.service;

import PodoeMarket.podoemarket.Utils.EntityToDTOConverter;
import PodoeMarket.podoemarket.dto.ProductDTO;
import PodoeMarket.podoemarket.dto.response.ProductListDTO;
import PodoeMarket.podoemarket.entity.*;
import PodoeMarket.podoemarket.repository.OrderItemRepository;
import PodoeMarket.podoemarket.repository.ProductRepository;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.S3Object;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
    private final AmazonS3 amazonS3;

    @Value("${cloud.aws.s3.url}")
    private String bucketURL;

    public List<ProductListDTO> longPlayList() {
        final List<ProductEntity> longPlays = productRepo.findAllByPlayTypeAndChecked(1, true);

        return longPlays.stream()
                .map(entity -> EntityToDTOConverter.convertToProductList(entity, bucketURL))
                .collect(Collectors.toList());
    }

    public List<ProductListDTO> shortPlayList() {
        final List<ProductEntity> shortPlays = productRepo.findAllByPlayTypeAndChecked(2, true);

        return shortPlays.stream()
                .map(entity -> EntityToDTOConverter.convertToProductList(entity, bucketURL))
                .collect(Collectors.toList());
    }

    public ProductEntity product(UUID id) {
        return productRepo.findById(id);
    }

    public boolean isBuyScript(UUID userId, UUID productId) {
        final List<OrderItemEntity> orderitems = orderItemRepo.findByProductIdAndUserId(productId, userId);

        for(OrderItemEntity item : orderitems) {
            if(item.isScript()) {
                return true;
            }
        }
        return false;
    }

    public byte[] scriptPreview(ProductEntity product) {
        S3Object s3Object = amazonS3.getObject("podobucket", product.getFilePath());

        try (InputStream src = s3Object.getObjectContent();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            PdfDocument pdfDoc = new PdfDocument(new PdfReader(src));
            PdfDocument newPdf = new PdfDocument(new PdfWriter(outputStream));

            int totalPages = pdfDoc.getNumberOfPages();

            if (product.getPlayType() == 1) { // 장편극
                for(int page = 1; page <= Math.min(3, totalPages); page ++) {
                    newPdf.addPage(pdfDoc.getPage(page).copyTo(newPdf));
                }
            } else if (product.getPlayType() == 2) { // 단편극
                if (totalPages >= 1)
                    newPdf.addPage(pdfDoc.getPage(1).copyTo(newPdf));
            }

            return outputStream.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("미리보기 오류 발생");
        }
    }

    public ProductDTO productDetail(UUID productId, boolean isBuyScript) {
        final ProductEntity script = product(productId);
        final ProductDTO productDTO = EntityToDTOConverter.convertToSingleProductDTO(script, isBuyScript, bucketURL);
        productDTO.setPreview(scriptPreview(script));

        return productDTO;
    }
}
