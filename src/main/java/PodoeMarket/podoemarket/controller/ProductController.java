package PodoeMarket.podoemarket.controller;

import PodoeMarket.podoemarket.dto.*;
import PodoeMarket.podoemarket.entity.*;
import PodoeMarket.podoemarket.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;
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
            // 작품 상세 정보
            ProductDTO productInfo = productService.productDetail(productId, userInfo.getId());

            // 리뷰
            
            // 문의
            List<ProductQnADTO> productQnA = productService.getProductQnA(productId);

            ProductDetailDTO res = new ProductDetailDTO(productInfo, productQnA);

            return ResponseEntity.ok().body(res);
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
                productService.likeDelete(userInfo.getId(), dto.getId());

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

    @GetMapping("/basket")
    public ResponseEntity<?> basketList(@AuthenticationPrincipal UserEntity userInfo) {
        try{
            return ResponseEntity.ok().body(productService.getAllBasketProducts(userInfo.getId()));
        } catch(Exception e) {
            ResponseDTO resDTO = ResponseDTO.builder().error(e.getMessage()).build();
            return ResponseEntity.badRequest().body(resDTO);
        }
    }

    @PostMapping("/basket")
    public ResponseEntity<?> productBasket(@AuthenticationPrincipal UserEntity userInfo, @RequestBody ProductDTO dto) {
        try {
            boolean isBasket = productService.isBasket(userInfo.getId(), dto.getId());

            if(isBasket) { // 장바구니 담기 취소
                productService.basketDelete(userInfo.getId(), dto.getId());

                return ResponseEntity.ok().body("basket delete");
            } else { // 장바구니 담기
                ProductEntity product = productService.product(dto.getId());

                BasketEntity basket = BasketEntity.builder()
                        .user(userInfo)
                        .product(product)
                        .build();

                productService.basketCreate(basket);

                return ResponseEntity.ok().body("basket success");
            }
        } catch (Exception e) {
            ResponseDTO resDTO = ResponseDTO.builder().error(e.getMessage()).build();
            return ResponseEntity.badRequest().body(resDTO);
        }
    }

    @PostMapping("/question")
    public ResponseEntity<?> writeQuestion(@AuthenticationPrincipal UserEntity userInfo, @RequestBody ProductQnADTO dto) {
        try {
            ProductEntity product = productService.product(dto.getProductId());

            ProductQnAEntity question = ProductQnAEntity.builder()
                    .user(userInfo)
                    .title(dto.getTitle())
                    .question(dto.getQuestion())
                    .date(dto.getDate())
                    .product(product)
                    .build();

            productService.writeQuestion(question, product.getUser().getId());

            return ResponseEntity.ok().body("question register");
        } catch (Exception e) {
            ResponseDTO resDTO = ResponseDTO.builder().error(e.getMessage()).build();
            return ResponseEntity.badRequest().body(resDTO);
        }
    }
}
