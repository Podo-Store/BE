package PodoeMarket.podoemarket.controller;

import PodoeMarket.podoemarket.dto.*;
import PodoeMarket.podoemarket.dto.response.ProductListDTO;
import PodoeMarket.podoemarket.dto.response.ResponseDTO;
import PodoeMarket.podoemarket.dto.response.ScriptListDTO;
import PodoeMarket.podoemarket.entity.*;
import PodoeMarket.podoemarket.service.ProductService;
import PodoeMarket.podoemarket.service.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.io.InputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RequiredArgsConstructor
@Controller
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
            // 로그인한 유저가 해당 작품의 대본을 구매한 이력이 있는지 확인
            boolean isBuyScript = userInfo != null && productService.isBuyScript(userInfo.getId(), productId);
            final ProductDTO productInfo = productService.productDetail(productId, isBuyScript);

            return ResponseEntity.ok().body(productInfo);
        } catch(Exception e) {
            ResponseDTO resDTO = ResponseDTO.builder().error(e.getMessage()).build();
            return ResponseEntity.badRequest().body(resDTO);
        }
    }

    @GetMapping("/preview")
    public ResponseEntity<?> scriptPreview(@RequestParam("script") UUID productId) {
        try {
            final String s3Key = productService.product(productId).getFilePath();

            final String preSignedURL = s3Service.generatePreSignedURL(s3Key);
            final InputStream fileStream = new URL(preSignedURL).openStream();

            // 파일 이름 추출 및 인코딩
            String fileName = s3Key.substring(s3Key.lastIndexOf('/') + 1);
            String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8.toString()).replace("+", "%20");

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF) // PDF 타입 명시
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename*=UTF-8''" + encodedFileName) // RFC 5987 형식
                    .body(new InputStreamResource(fileStream));
        } catch(Exception e) {
            ResponseDTO resDTO = ResponseDTO.builder().error(e.getMessage()).build();
            return ResponseEntity.badRequest().body(resDTO);
        }
    }
}
