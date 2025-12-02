package PodoeMarket.podoemarket.order.controller;

import PodoeMarket.podoemarket.common.entity.*;
import PodoeMarket.podoemarket.order.dto.request.OrderInfoRequestDTO;
import PodoeMarket.podoemarket.order.dto.request.OrderRequestDTO;
import PodoeMarket.podoemarket.common.dto.ResponseDTO;
import PodoeMarket.podoemarket.order.dto.response.OrderCompleteResponseDTO;
import PodoeMarket.podoemarket.order.dto.response.OrderInfoResponseDTO;
import PodoeMarket.podoemarket.order.dto.response.OrderItemResponseDTO;
import PodoeMarket.podoemarket.order.service.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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

    @GetMapping("/item")
    public ResponseEntity<?> getPurchaseInfo(@AuthenticationPrincipal UserEntity userInfo, @ModelAttribute OrderInfoRequestDTO dto) {
        try {
            // 어드민이 인가된 계정만 결제 창 진입 가능
            if(!userInfo.isAuth())
                throw new RuntimeException("어드민 권한이 존재하지 않습니다.");

            OrderItemResponseDTO item = orderService.getOrderItemInfo(userInfo, dto);

            return ResponseEntity.ok().body(item);
        } catch (Exception e) {
            ResponseDTO resDTO = ResponseDTO.builder().error(e.getMessage()).build();
            return ResponseEntity.badRequest().body(resDTO);
        }
    }

    @PostMapping("/item")
    public ResponseEntity<?> purchase(@AuthenticationPrincipal UserEntity userInfo,
                                      @RequestBody OrderRequestDTO dto,
                                      HttpServletRequest req,
                                      HttpServletResponse res) {
        try {
            String resultCode = req.getParameter("resultCode");
            String tid = req.getParameter("tid");

            log.info("dto: {}", dto.toString());
            log.info("resultCode = {}, tid = {}", resultCode, tid);

            if (!"0000".equals(resultCode))
                res.sendRedirect("https://www.podo-store.com/purchase/abort");

            List<OrderCompleteResponseDTO> resDTO = orderService.purchaseProduct(userInfo, dto, req.getParameter("tid"));
            res.sendRedirect("https://podo-store.com/purchase/success");

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
