package PodoeMarket.podoemarket.controller;

import PodoeMarket.podoemarket.dto.*;
import PodoeMarket.podoemarket.dto.response.ResponseDTO;
import PodoeMarket.podoemarket.dto.response.ScriptListDTO;
import PodoeMarket.podoemarket.entity.*;
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
@RequestMapping("/scripts")
public class ProductController {
    private final ProductService productService;

    @GetMapping
    public ResponseEntity<?> allProducts() {
        try{
            final ScriptListDTO lists = new ScriptListDTO(productService.longPlayList(), productService.shortPlayList());

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
            boolean isBuyScript = false;

            if (userInfo != null) // 로그인 시
                 isBuyScript = productService.isBuyScript(userInfo.getId(), productId);

            final ProductDTO productInfo = productService.productDetail(productId, isBuyScript);

            return ResponseEntity.ok().body(productInfo);
        } catch(Exception e) {
            ResponseDTO resDTO = ResponseDTO.builder().error(e.getMessage()).build();
            return ResponseEntity.badRequest().body(resDTO);
        }
    }
}
