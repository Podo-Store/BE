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
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@Controller
@Slf4j
@RequestMapping("/order")
public class OrderController {
    private final OrderService orderService;
    private final ProductService productService;

    @GetMapping("/item")
    public ResponseEntity<?> getPurchaseInfo(@ModelAttribute OrderItemDTO dto) {
        try {
            ProductEntity orderProduct = productService.product(dto.getProductId());

            if (orderProduct == null) {
                ResponseDTO resDTO = ResponseDTO.builder()
                        .error("작품 정보 조회 실패")
                        .build();

                return ResponseEntity.badRequest().body(resDTO);
            }

            int totalPrice = (dto.isScript() ? orderProduct.getScriptPrice() : 0) + (dto.isPerformance() ? orderProduct.getPerformancePrice() : 0);

            OrderItemDTO item = OrderItemDTO.builder()
                    .title(orderProduct.getTitle())
                    .writer(orderProduct.getWriter())
                    .imagePath(orderProduct.getImagePath())
                    .playType(orderProduct.getPlayType())
                    .script(dto.isScript())
                    .scriptPrice(orderProduct.getScriptPrice())
                    .performance(dto.isPerformance())
                    .performancePrice(orderProduct.getPerformancePrice())
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
            OrdersEntity order = OrdersEntity.builder()
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
