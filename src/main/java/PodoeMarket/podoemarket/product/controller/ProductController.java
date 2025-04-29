package PodoeMarket.podoemarket.product.controller;

import PodoeMarket.podoemarket.common.entity.ProductEntity;
import PodoeMarket.podoemarket.common.entity.ProductLikeEntity;
import PodoeMarket.podoemarket.common.entity.UserEntity;
import PodoeMarket.podoemarket.dto.response.ResponseDTO;
import PodoeMarket.podoemarket.product.dto.response.ScriptDetailResponseDTO;
import PodoeMarket.podoemarket.product.dto.response.ScriptListResponseDTO;
import PodoeMarket.podoemarket.common.entity.type.PlayType;
import PodoeMarket.podoemarket.product.service.ProductService;
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
    public ResponseEntity<?> allProducts(@AuthenticationPrincipal UserEntity userInfo) {
        try{
            final ScriptListResponseDTO lists = new ScriptListResponseDTO(productService.getPlayList(0, userInfo, PlayType.LONG, 10), productService.getPlayList(0, userInfo, PlayType.SHORT, 20));

            return ResponseEntity.ok().body(lists);
        } catch(Exception e) {
            ResponseDTO resDTO = ResponseDTO.builder().error(e.getMessage()).build();
            return ResponseEntity.badRequest().body(resDTO);
        }
    }

    @GetMapping("/long")
    public ResponseEntity<?> longProducts(@AuthenticationPrincipal UserEntity userInfo, @RequestParam(value = "page", defaultValue = "0") int page) {
        try{
            final List<ScriptListResponseDTO.ProductListDTO> lists = productService.getPlayList(page, userInfo, PlayType.LONG, 20);

            return ResponseEntity.ok().body(lists);
        } catch(Exception e) {
            ResponseDTO resDTO = ResponseDTO.builder().error(e.getMessage()).build();
            return ResponseEntity.badRequest().body(resDTO);
        }
    }

    @GetMapping("/short")
    public ResponseEntity<?> shortProducts(@AuthenticationPrincipal UserEntity userInfo, @RequestParam(value = "page", defaultValue = "0") int page) {
        try{
            final List<ScriptListResponseDTO.ProductListDTO> lists = productService.getPlayList(page, userInfo, PlayType.SHORT, 20);

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
            // 로그인한 유저의 좋아요 여부 확인
            final boolean likeStatus = productService.getLikeStatus(userInfo, productId);
            // 총 좋아요 수
            final int likeCount = productService.getLikeCount(productId);
            final ScriptDetailResponseDTO productInfo = productService.productDetail(productId, buyStatus, likeStatus, likeCount);

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

    @PostMapping("/like/{id}")
    public ResponseEntity<?> productLike(@AuthenticationPrincipal UserEntity userInfo, @PathVariable UUID id) {
        try{
            final boolean likeStatus = productService.getLikeStatus(userInfo, id);

            if (likeStatus) {
                productService.deleteLike(userInfo, id);

                return ResponseEntity.ok().body("cancel like");
            } else {
                final ProductEntity product = productService.product(id);

                final ProductLikeEntity like = ProductLikeEntity.builder()
                        .user(userInfo)
                        .product(product)
                        .build();

                productService.createLike(like);

                return ResponseEntity.ok().body("like");
            }
        } catch(Exception e) {
            ResponseDTO resDTO = ResponseDTO.builder().error(e.getMessage()).build();
            return ResponseEntity.badRequest().body(resDTO);
        }
    }
}
