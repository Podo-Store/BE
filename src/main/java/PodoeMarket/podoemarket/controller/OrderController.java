package PodoeMarket.podoemarket.controller;

import PodoeMarket.podoemarket.dto.OrderDTO;
import PodoeMarket.podoemarket.dto.OrderItemDTO;
import PodoeMarket.podoemarket.dto.ResponseDTO;
import PodoeMarket.podoemarket.entity.OrdersEntity;
import PodoeMarket.podoemarket.entity.ProductEntity;
import PodoeMarket.podoemarket.entity.UserEntity;
import PodoeMarket.podoemarket.service.OrderService;
import PodoeMarket.podoemarket.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;

@RequiredArgsConstructor
@Controller
@Slf4j
@RequestMapping("/order")
public class OrderController {
    private final OrderService orderService;
    private final ProductService productService;

    @Value("${cloud.aws.s3.url}")
    private String bucketURL;

    @GetMapping("/item")
    public ResponseEntity<?> getPurchaseInfo(@AuthenticationPrincipal UserEntity userInfo, @ModelAttribute OrderItemDTO dto) {
        try {
            final ProductEntity orderProduct = productService.product(dto.getProductId());

            if(userInfo.getId().equals(orderProduct.getUser().getId())) {
                ResponseDTO resDTO = ResponseDTO.builder()
                        .error("본인 작품 구매 불가")
                        .build();

                return ResponseEntity.badRequest().body(resDTO);
            }

            if(orderProduct == null) {
                ResponseDTO resDTO = ResponseDTO.builder()
                        .error("작품 정보 조회 실패")
                        .build();

                return ResponseEntity.badRequest().body(resDTO);
            }

            final int totalPrice = (dto.isScript() ? orderProduct.getScriptPrice() : 0) + (dto.isPerformance() ? orderProduct.getPerformancePrice() : 0);
            final String encodedScriptImage = orderProduct.getImagePath() != null ? bucketURL + URLEncoder.encode(orderProduct.getImagePath(), "UTF-8") : "";

            OrderItemDTO item = OrderItemDTO.builder()
                    .title(orderProduct.getTitle())
                    .writer(orderProduct.getWriter())
                    .imagePath(encodedScriptImage)
                    .playType(orderProduct.getPlayType())
                    .script(dto.isScript())
                    .scriptPrice(dto.isScript() ? orderProduct.getScriptPrice() : 0)
                    .performance(dto.isPerformance())
                    .performancePrice(dto.isPerformance() ? orderProduct.getPerformancePrice() : 0)
                    .totalPrice(totalPrice)
                    .build();

            return ResponseEntity.ok().body(item);
        } catch (Exception e) {
            ResponseDTO resDTO = ResponseDTO.builder().error(e.getMessage()).build();
            return ResponseEntity.badRequest().body(resDTO);
        }
    }

    @PostMapping("/item")
    public ResponseEntity<?> purchase(@AuthenticationPrincipal UserEntity userInfo, @RequestBody OrderDTO dto) {
        try {
            final OrdersEntity order = OrdersEntity.builder()
                    .user(userInfo)
                    .build();

            orderService.orderCreate(order, dto, userInfo);

            return ResponseEntity.ok().body(true);
        } catch(Exception e) {
            ResponseDTO resDTO = ResponseDTO.builder().error(e.getMessage()).build();
            return ResponseEntity.badRequest().body(resDTO);
        }
    }
}
