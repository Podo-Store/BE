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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Slf4j
@Service
public class ProductService {
    private final ProductRepository productRepo;
    private final OrderItemRepository orderItemRepo;
    private final S3Service s3Service;

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
            if(item.isScript()) {
                return true;
            }
        }
        return false;
    }

    public ProductDTO productDetail(UUID productId, boolean isBuyScript) throws IOException {
        final ProductEntity script = product(productId);
        log.info("script:{}", script);

        final String preSignedURL = s3Service.generatePreSignedURL(script.getFilePath());
        final InputStream fileStream = new URL(preSignedURL).openStream();

        return EntityToDTOConverter.convertToSingleProductDTO(script, isBuyScript, bucketURL, fileStream);
    }
}
