package PodoeMarket.podoemarket.product.controller;

import PodoeMarket.podoemarket.common.entity.ProductEntity;
import PodoeMarket.podoemarket.common.entity.UserEntity;
import PodoeMarket.podoemarket.common.dto.ResponseDTO;
import PodoeMarket.podoemarket.product.dto.request.ReviewRequestDTO;
import PodoeMarket.podoemarket.product.dto.response.ScriptDetailResponseDTO;
import PodoeMarket.podoemarket.product.dto.response.ScriptListResponseDTO;
import PodoeMarket.podoemarket.common.entity.type.PlayType;
import PodoeMarket.podoemarket.product.service.ProductService;
import PodoeMarket.podoemarket.product.type.ProductSortType;
import PodoeMarket.podoemarket.product.type.ReviewSortType;
import PodoeMarket.podoemarket.service.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.nio.charset.StandardCharsets;
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
    public ResponseEntity<?> allProducts(@AuthenticationPrincipal UserEntity userInfo, @RequestParam(defaultValue = "POPULAR") ProductSortType sortType) {
        try{
            final ScriptListResponseDTO lists = new ScriptListResponseDTO(
                    productService.getPlayList(0, userInfo, PlayType.LONG, 10, sortType),
                    productService.getPlayList(0, userInfo, PlayType.SHORT, 10, sortType)
            );

            return ResponseEntity.ok().body(lists);
        } catch(Exception e) {
            ResponseDTO resDTO = ResponseDTO.builder().error(e.getMessage()).build();
            return ResponseEntity.badRequest().body(resDTO);
        }
    }

    @GetMapping("/long")
    public ResponseEntity<?> longProducts(@AuthenticationPrincipal UserEntity userInfo, @RequestParam(value = "page", defaultValue = "0") int page, @RequestParam(defaultValue = "POPULAR") ProductSortType sortType) {
        try{
            final List<ScriptListResponseDTO.ProductListDTO> lists = productService.getPlayList(page, userInfo, PlayType.LONG, 20, sortType);

            return ResponseEntity.ok().body(lists);
        } catch(Exception e) {
            ResponseDTO resDTO = ResponseDTO.builder().error(e.getMessage()).build();
            return ResponseEntity.badRequest().body(resDTO);
        }
    }

    @GetMapping("/short")
    public ResponseEntity<?> shortProducts(@AuthenticationPrincipal UserEntity userInfo, @RequestParam(value = "page", defaultValue = "0") int page, @RequestParam(defaultValue = "POPULAR") ProductSortType sortType) {
        try{
            final List<ScriptListResponseDTO.ProductListDTO> lists = productService.getPlayList(page, userInfo, PlayType.SHORT, 20, sortType);

            return ResponseEntity.ok().body(lists);
        } catch(Exception e) {
            ResponseDTO resDTO = ResponseDTO.builder().error(e.getMessage()).build();
            return ResponseEntity.badRequest().body(resDTO);
        }
    }

    @GetMapping("/detail")
    public ResponseEntity<?> scriptInfo(@AuthenticationPrincipal UserEntity userInfo,
                                        @RequestParam("script") UUID productId,
                                        @RequestParam(defaultValue = "LIKE_COUNT") ReviewSortType sortType,
                                        @RequestParam(value = "page", defaultValue = "0") int page) {
        try{
            final ScriptDetailResponseDTO productInfo = productService.getScriptDetailInfo(userInfo, productId, page, 5, sortType);

            return ResponseEntity.ok().body(productInfo);
        } catch(Exception e) {
            ResponseDTO resDTO = ResponseDTO.builder().error(e.getMessage()).build();
            return ResponseEntity.badRequest().body(resDTO);
        }
    }

    @GetMapping("/preview")
    public ResponseEntity<StreamingResponseBody> scriptPreview(@RequestParam("script") UUID productId) {
        try {
            // 데이터베이스 작업 (트랜잭션 내에서 수행)
            final ProductEntity product = productService.getProduct(productId);
            final String s3Key = product.getFilePath();
            final String preSignedURL = s3Service.generatePreSignedURL(s3Key);
            int pagesToExtract = (product.getPlayType() == PlayType.LONG) ? 3 : 1;

            // PDF 처리 (트랜잭션 외부에서 수행)
            return productService.generateScriptPreview(preSignedURL, pagesToExtract);
        } catch(Exception e) {
            StreamingResponseBody errorBody = outputStream -> {
                String errorMsg = "{\"error\": \"" + e.getMessage() + "\"}";
                outputStream.write(errorMsg.getBytes(StandardCharsets.UTF_8));
                outputStream.flush();
            };
            return ResponseEntity
                    .badRequest()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(errorBody);
        }
    }

    @GetMapping("/description")
    public ResponseEntity<StreamingResponseBody> descriptionView(@RequestParam("script") UUID productId) {
        try{
            // 데이터베이스 작업 (트랜잭션 내에서 수행)
            final ProductEntity product = productService.getProduct(productId);
            final String s3Key = product.getDescriptionPath();

            if (s3Key == null)
                throw new RuntimeException("false");

            final String preSignedURL = s3Service.generatePreSignedURL(s3Key);

            return productService.generateFullPDF(preSignedURL);
        } catch(Exception e) {
                StreamingResponseBody errorBody = outputStream -> {
                    String errorMsg = "{\"error\": \"" + e.getMessage() + "\"}";
                    outputStream.write(errorMsg.getBytes(StandardCharsets.UTF_8));
                    outputStream.flush();
                };
                return ResponseEntity
                        .badRequest()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(errorBody);
            }
    }

    @PostMapping("/like/{id}")
    public ResponseEntity<?> productLike(@AuthenticationPrincipal UserEntity userInfo, @PathVariable UUID id) {
        try{
            if (userInfo == null) {
                ResponseDTO resDTO = ResponseDTO.builder()
                        .error("로그인이 필요한 서비스입니다.")
                        .build();

                return ResponseEntity.badRequest().body(resDTO);
            }

            String result = productService.toggleLike(userInfo, id);

            return ResponseEntity.ok().body(result);
        } catch(Exception e) {
            ResponseDTO resDTO = ResponseDTO.builder().error(e.getMessage()).build();
            return ResponseEntity.badRequest().body(resDTO);
        }
    }

    @GetMapping("/view")
    public ResponseEntity<StreamingResponseBody> scriptView(@RequestParam("script") UUID productId) {
        try{
            // 데이터베이스 작업 (트랜잭션 내에서 수행)
            final ProductEntity product = productService.getProduct(productId);
            final String s3Key = product.getFilePath();
            final String preSignedURL = s3Service.generatePreSignedURL(s3Key);

            return productService.generateFullPDF(preSignedURL);
        } catch(Exception e) {
            StreamingResponseBody errorBody = outputStream -> {
                String errorMsg = "{\"error\": \"" + e.getMessage() + "\"}";
                outputStream.write(errorMsg.getBytes(StandardCharsets.UTF_8));
                outputStream.flush();
            };
            return ResponseEntity
                    .badRequest()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(errorBody);
        }
    }

    @GetMapping("/likeStatus/{id}")
    public ResponseEntity<?> likeStatus(@AuthenticationPrincipal UserEntity userInfo, @PathVariable UUID id) {
        try{
            return ResponseEntity.ok().body(productService.getProductLikeStatus(userInfo, id));
        } catch(Exception e) {
            ResponseDTO resDTO = ResponseDTO.builder().error(e.getMessage()).build();
            return ResponseEntity.badRequest().body(resDTO);
        }
    }

    @PostMapping("/review")
    public ResponseEntity<?> writeReview(@AuthenticationPrincipal UserEntity userInfo, @RequestBody ReviewRequestDTO dto) {
        try {
            productService.writeReview(userInfo, dto);

            return ResponseEntity.ok().body(true);
        } catch (Exception e) {
            ResponseDTO resDTO = ResponseDTO.builder().error(e.getMessage()).build();
            return ResponseEntity.badRequest().body(resDTO);
        }
    }

    @DeleteMapping("/review/{id}")
    public ResponseEntity<?> deleteReview(@AuthenticationPrincipal UserEntity userInfo, @PathVariable UUID id) {
        try {
            productService.deleteReview(userInfo, id);

            return ResponseEntity.ok().body(true);
        } catch (Exception e) {
            ResponseDTO resDTO = ResponseDTO.builder().error(e.getMessage()).build();
            return ResponseEntity.badRequest().body(resDTO);
        }
    }
}
