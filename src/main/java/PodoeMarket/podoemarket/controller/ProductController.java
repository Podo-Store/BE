package PodoeMarket.podoemarket.controller;

import PodoeMarket.podoemarket.Utils.EntityToDTOConverter;
import PodoeMarket.podoemarket.dto.ProductDTO;
import PodoeMarket.podoemarket.dto.ResponseDTO;
import PodoeMarket.podoemarket.entity.ProductEntity;
import PodoeMarket.podoemarket.entity.ProductLikeEntity;
import PodoeMarket.podoemarket.entity.UserEntity;
import PodoeMarket.podoemarket.service.MypageService;
import PodoeMarket.podoemarket.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RequiredArgsConstructor
@Controller
@Slf4j
@RequestMapping("/product")
public class ProductController {
    private final ProductService productService;

    @GetMapping("/scriptDetail")
    public ResponseEntity<?> scriptInfo(@RequestParam("script") UUID productId, @AuthenticationPrincipal UserEntity userInfo) {
        try{
            ProductDTO productInfo = productService.productDetail(productId, userInfo.getId());

            return ResponseEntity.ok().body(productInfo);
        } catch(Exception e) {
            ResponseDTO resDTO = ResponseDTO.builder().error(e.getMessage()).build();
            return ResponseEntity.badRequest().body(resDTO);
        }
    }

    @PostMapping("/like")
    public ResponseEntity<?> productLike(@AuthenticationPrincipal UserEntity userInfo, @RequestBody ProductDTO dto) {
        try {
            boolean isLike = productService.isLike(userInfo.getId(), dto.getId());

            if (isLike) { // 좋아요 취소
                productService.delete(userInfo.getId(), dto.getId());

                return ResponseEntity.ok().body("like delete");
            } else { // 좋아요 생성
                ProductEntity product = productService.product(dto.getId());

                ProductLikeEntity like = ProductLikeEntity.builder()
                        .user(userInfo)
                        .product(product)
                        .build();

                productService.likeCreate(like);

                return ResponseEntity.ok().body("like success");
            }
        } catch (Exception e) {
            ResponseDTO resDTO = ResponseDTO.builder().error(e.getMessage()).build();
            return ResponseEntity.badRequest().body(resDTO);
        }
    }
}