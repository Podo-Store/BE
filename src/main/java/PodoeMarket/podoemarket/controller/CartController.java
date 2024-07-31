package PodoeMarket.podoemarket.controller;

import PodoeMarket.podoemarket.dto.CartDTO;
import PodoeMarket.podoemarket.dto.ProductDTO;
import PodoeMarket.podoemarket.dto.ResponseDTO;
import PodoeMarket.podoemarket.entity.CartEntity;
import PodoeMarket.podoemarket.entity.ProductEntity;
import PodoeMarket.podoemarket.entity.UserEntity;
import PodoeMarket.podoemarket.service.CartService;
import PodoeMarket.podoemarket.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@RequiredArgsConstructor
@Controller
@Slf4j
@RequestMapping("/cart")
public class CartController {
    private final CartService cartService;
    private final ProductService productService;

    @GetMapping
    public ResponseEntity<?> basketList(@AuthenticationPrincipal UserEntity userInfo) {
        try{
            return ResponseEntity.ok().body(cartService.getAllCartProducts(userInfo.getId()));
        } catch(Exception e) {
            ResponseDTO resDTO = ResponseDTO.builder().error(e.getMessage()).build();
            return ResponseEntity.badRequest().body(resDTO);
        }
    }

    @PostMapping("/add")
    public ResponseEntity<?> productBasket(@AuthenticationPrincipal UserEntity userInfo, @RequestBody CartDTO dto) {
        try {
            CartEntity cartProduct = cartService.product(userInfo.getId(), dto.getProductId());

            if (cartProduct == null) {
                log.info("info is null");
            }

//            boolean isBasket = cartService.isCart(userInfo.getId(), dto.getProductId());
//
//            if(isBasket) { // 장바구니 담기 취소
//                cartService.cartDelete(userInfo.getId(), dto.getProductId());
//
//                return ResponseEntity.ok().body("basket delete");
//            } else { // 장바구니 담기
//                ProductEntity product = productService.product(dto.getProductId());
//
//                CartEntity basket = CartEntity.builder()
//                        .status(dto.getStatus())
//                        .user(userInfo)
//                        .product(product)
//                        .build();
//
//                cartService.cartCreate(basket);
//
//                return ResponseEntity.ok().body("basket success");
//            }
            return ResponseEntity.ok().body(true);
        } catch (Exception e) {
            ResponseDTO resDTO = ResponseDTO.builder().error(e.getMessage()).build();
            return ResponseEntity.badRequest().body(resDTO);
        }
    }
}
