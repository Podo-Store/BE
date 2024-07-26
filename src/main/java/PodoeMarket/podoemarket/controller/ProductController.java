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
            ProductDTO productInfo = productService.productDetail(productId, userInfo.getId());

            return ResponseEntity.ok().body(productInfo);
        } catch(Exception e) {
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
}
