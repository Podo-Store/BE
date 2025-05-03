package PodoeMarket.podoemarket.order.controller;

import PodoeMarket.podoemarket.common.entity.*;
import PodoeMarket.podoemarket.common.entity.type.OrderStatus;
import PodoeMarket.podoemarket.dto.response.OrderCompleteDTO;
import PodoeMarket.podoemarket.dto.OrderDTO;
import PodoeMarket.podoemarket.dto.response.OrderItemDTO;
import PodoeMarket.podoemarket.dto.response.ResponseDTO;
import PodoeMarket.podoemarket.order.dto.response.OrderInfoResponseDTO;
import PodoeMarket.podoemarket.service.MailSendService;
import PodoeMarket.podoemarket.order.service.OrderService;
import PodoeMarket.podoemarket.product.service.ProductService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.util.List;

@RequiredArgsConstructor
@RestController
@Slf4j
@RequestMapping("/order")
public class OrderController {
    private final OrderService orderService;
    private final ProductService productService;
    private final MailSendService mailSendService;

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

            final int totalPrice = (dto.isScript() ? orderProduct.getScriptPrice() : 0) + (dto.getPerformanceAmount() > 0 ? orderProduct.getPerformancePrice() * dto.getPerformanceAmount() : 0);
            final String encodedScriptImage = orderProduct.getImagePath() != null ? bucketURL + URLEncoder.encode(orderProduct.getImagePath(), "UTF-8") : "";

            final OrderItemDTO item = OrderItemDTO.builder()
                    .title(orderProduct.getTitle())
                    .writer(orderProduct.getWriter())
                    .imagePath(encodedScriptImage)
                    .playType(orderProduct.getPlayType())
                    .script(dto.isScript())
                    .scriptPrice(dto.isScript() ? orderProduct.getScriptPrice() : 0)
                    .performanceAmount(dto.getPerformanceAmount())
                    .performancePrice(orderProduct.getPerformancePrice())
                    .performanceTotalPrice(dto.getPerformanceAmount() > 0 ? orderProduct.getPerformancePrice() * dto.getPerformanceAmount() : 0)
                    .totalPrice(totalPrice)
                    .build();

            return ResponseEntity.ok().body(item);
        } catch (Exception e) {
            ResponseDTO resDTO = ResponseDTO.builder().error(e.getMessage()).build();
            return ResponseEntity.badRequest().body(resDTO);
        }
    }

    @Transactional
    @PostMapping("/item")
    public ResponseEntity<?> purchase(@AuthenticationPrincipal UserEntity userInfo, @RequestBody OrderDTO dto) {
        try {
            final OrdersEntity order = OrdersEntity.builder()
                    .user(userInfo)
                    .paymentMethod(dto.getPaymentMethod())
                    .orderStatus(OrderStatus.WAIT)
                    .build();

            final OrdersEntity orders = orderService.orderCreate(order, dto, userInfo);

            if(orderService.buyPerformance(orders.getId())) {
                final ApplicantEntity applicant = ApplicantEntity.builder()
                        .name(dto.getApplicant().getName())
                        .phoneNumber(dto.getApplicant().getPhoneNumber())
                        .address(dto.getApplicant().getAddress())
                        .orderItem(orders.getOrderItem().getFirst())
                        .build();

                orderService.createApplicant(applicant);
            }

            final List<OrderCompleteDTO> resDTO = orderService.orderResult(orders);

            return ResponseEntity.ok().body(resDTO);
        } catch(Exception e) {
            ResponseDTO resDTO = ResponseDTO.builder().error(e.getMessage()).build();
            return ResponseEntity.badRequest().body(resDTO);
        }
    }

    @GetMapping("/success")
    public ResponseEntity<?> purchaseSuccess(@RequestParam Long orderId) {
        try {
            final OrdersEntity order = orderService.getOrderInfo(orderId);
            final List<OrderItemEntity> orderItem = orderService.getOrderItem(orderId);

            final OrderInfoResponseDTO resDTO = OrderInfoResponseDTO.builder()
                    .orderId(orderId)
                    .orderDate(order.getCreatedAt())
                    .title(orderItem.getFirst().getTitle())
                    .script(orderItem.getFirst().getScript())
                    .scriptPrice(orderItem.getFirst().getScriptPrice())
                    .performanceAmount(orderItem.getFirst().getPerformanceAmount())
                    .performancePrice(orderItem.getFirst().getPerformancePrice())
                    .build();

            String formatPrice = orderService.formatPrice(order.getTotalPrice());

            mailSendService.joinPaymentEmail(order.getUser().getEmail(), formatPrice);

            return ResponseEntity.ok().body(resDTO);
        } catch(Exception e) {
            ResponseDTO resDTO = ResponseDTO.builder().error(e.getMessage()).build();
            return ResponseEntity.badRequest().body(resDTO);
        }
    }
}
