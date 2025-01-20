package PodoeMarket.podoemarket.controller;

import PodoeMarket.podoemarket.dto.*;
import PodoeMarket.podoemarket.dto.response.ProductListDTO;
import PodoeMarket.podoemarket.dto.response.ResponseDTO;
import PodoeMarket.podoemarket.dto.response.ScriptListDTO;
import PodoeMarket.podoemarket.entity.*;
import PodoeMarket.podoemarket.entity.type.PlayType;
import PodoeMarket.podoemarket.service.ProductService;
import PodoeMarket.podoemarket.service.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@RestController
@Slf4j
@RequestMapping("/scripts")
public class ProductController {
    private final ProductService productService;
    private final S3Service s3Service;

    @GetMapping
    public ResponseEntity<?> allProducts() {
        try{
            final ScriptListDTO lists = new ScriptListDTO(productService.mainLongPlayList(), productService.mainShortPlayList());

            return ResponseEntity.ok().body(lists);
        } catch(Exception e) {
            ResponseDTO resDTO = ResponseDTO.builder().error(e.getMessage()).build();
            return ResponseEntity.badRequest().body(resDTO);
        }
    }

    @GetMapping("/long")
    public ResponseEntity<?> longProducts(@RequestParam(value = "page", defaultValue = "0") int page) {
        try{
            final List<ProductListDTO> lists = productService.longPlayList(page);

            return ResponseEntity.ok().body(lists);
        } catch(Exception e) {
            ResponseDTO resDTO = ResponseDTO.builder().error(e.getMessage()).build();
            return ResponseEntity.badRequest().body(resDTO);
        }
    }


    @GetMapping("/short")
    public ResponseEntity<?> shortProducts(@RequestParam(value = "page", defaultValue = "0") int page) {
        try{
            final List<ProductListDTO> lists = productService.shortPlayList(page);

            return ResponseEntity.ok().body(lists);
        } catch(Exception e) {
            ResponseDTO resDTO = ResponseDTO.builder().error(e.getMessage()).build();
            return ResponseEntity.badRequest().body(resDTO);
        }
    }

    @GetMapping("/detail")
    public ResponseEntity<?> scriptInfo(@AuthenticationPrincipal UserEntity userInfo, @RequestParam("script") UUID productId) {
        try{
            // 로그인한 유저의 해당 작품 구매 이력 확인
            final int buyStatus = productService.buyStatus(userInfo, productId);
            final ProductDTO productInfo = productService.productDetail(productId, buyStatus);

            return ResponseEntity.ok().body(productInfo);
        } catch(Exception e) {
            ResponseDTO resDTO = ResponseDTO.builder().error(e.getMessage()).build();
            return ResponseEntity.badRequest().body(resDTO);
        }
    }

    @GetMapping("/preview")
    public ResponseEntity<StreamingResponseBody> scriptPreview(@RequestParam("script") UUID productId) {
        try {
            final ProductEntity product = productService.product(productId);
            final String s3Key = product.getFilePath();
            final String preSignedURL = s3Service.generatePreSignedURL(s3Key);

            int pagesToExtract = (product.getPlayType() == PlayType.LONG) ? 3 : 1;

            try(InputStream fileStream = new URL(preSignedURL).openStream()) {
                ProductService.PdfExtractionResult result = productService.extractPagesFromPdf(fileStream, pagesToExtract);

                StreamingResponseBody streamingResponseBody = outputStream -> {
                    try (InputStream extractedPdfStream = new ByteArrayInputStream(result.getExtractedPdfBytes())) {
                        byte[] buffer = new byte[8192];
                        int bytesRead;
                        while ((bytesRead = extractedPdfStream.read(buffer)) != -1) {
                            outputStream.write(buffer, 0, bytesRead);
                            outputStream.flush();
                        }
                    }
                };

                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_PDF) // PDF 타입 명시
                        .header("X-Total-Pages", String.valueOf(result.getTotalPageCount()))
                        .body(streamingResponseBody);
            }
        } catch(Exception e) {
            ResponseDTO resDTO = ResponseDTO.builder().error(e.getMessage()).build();
            return ResponseEntity.badRequest().body((StreamingResponseBody) resDTO);
        }
    }
}
