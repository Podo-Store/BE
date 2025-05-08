package PodoeMarket.podoemarket.order.controller;

import PodoeMarket.podoemarket.common.entity.*;
import PodoeMarket.podoemarket.order.dto.request.OrderRequestDTO;
import PodoeMarket.podoemarket.dto.ResponseDTO;
import PodoeMarket.podoemarket.order.dto.response.OrderCompleteResponseDTO;
import PodoeMarket.podoemarket.order.dto.response.OrderInfoResponseDTO;
import PodoeMarket.podoemarket.order.dto.response.OrderItemResponseDTO;
import PodoeMarket.podoemarket.service.MailSendService;
import PodoeMarket.podoemarket.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@Slf4j
@RequestMapping("/order")
public class OrderController {
    private final OrderService orderService;
    private final MailSendService mailSendService;

    @GetMapping("/item")
    public ResponseEntity<?> getPurchaseInfo(@AuthenticationPrincipal UserEntity userInfo, @ModelAttribute OrderItemResponseDTO dto) {
        try {
            OrderItemResponseDTO item = orderService.getOrderItemInfo(userInfo, dto);

            return ResponseEntity.ok().body(item);
        } catch (Exception e) {
            ResponseDTO resDTO = ResponseDTO.builder().error(e.getMessage()).build();
            return ResponseEntity.badRequest().body(resDTO);
        }
    }

    @PostMapping("/item")
    public ResponseEntity<?> purchase(@AuthenticationPrincipal UserEntity userInfo, @RequestBody OrderRequestDTO dto) {
        try {
            List<OrderCompleteResponseDTO> resDTO = orderService.purchaseProduct(userInfo, dto);

            return ResponseEntity.ok().body(resDTO);
        } catch(Exception e) {
            ResponseDTO resDTO = ResponseDTO.builder().error(e.getMessage()).build();
            return ResponseEntity.badRequest().body(resDTO);
        }
    }

    @GetMapping("/success")
    public ResponseEntity<?> purchaseSuccess(@RequestParam Long orderId) {
        try {
            OrderInfoResponseDTO resDTO = orderService.orderSuccess(orderId);

            return ResponseEntity.ok().body(resDTO);
        } catch(Exception e) {
            ResponseDTO resDTO = ResponseDTO.builder().error(e.getMessage()).build();
            return ResponseEntity.badRequest().body(resDTO);
        }
    }
}
