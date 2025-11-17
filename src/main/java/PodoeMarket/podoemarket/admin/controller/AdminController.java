package PodoeMarket.podoemarket.admin.controller;

import PodoeMarket.podoemarket.admin.dto.request.PlayTypeRequestDTO;
import PodoeMarket.podoemarket.admin.dto.request.UpdateTitleRequestDTO;
import PodoeMarket.podoemarket.admin.dto.request.UpdateWriterRequestDTO;
import PodoeMarket.podoemarket.admin.dto.response.OrderManagementResponseDTO;
import PodoeMarket.podoemarket.admin.dto.response.StatisticsResponseDTO;
import PodoeMarket.podoemarket.admin.service.AdminService;
import PodoeMarket.podoemarket.admin.dto.response.ProductManagementResponseDTO;
import PodoeMarket.podoemarket.common.dto.ResponseDTO;
import PodoeMarket.podoemarket.common.entity.ProductEntity;
import PodoeMarket.podoemarket.common.entity.UserEntity;
import PodoeMarket.podoemarket.common.entity.type.ProductStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@Slf4j
@RequestMapping("/admin")
@RestController
public class AdminController {
    private final AdminService adminService;

    @GetMapping("/products")
    public ResponseEntity<?> productManage(@AuthenticationPrincipal UserEntity userInfo,
                                           @RequestParam(value = "page", defaultValue = "0") int page,
                                           @RequestParam(value = "search", required = false, defaultValue = "") String search,
                                           @RequestParam(value = "status", required = false) ProductStatus status) {
        try {
            adminService.checkAuth(userInfo);

            final Long productPassCnt = adminService.getCheckedCount(ProductStatus.PASS) + adminService.getCheckedCount(ProductStatus.RE_PASS);
            final Long productWaitCnt = adminService.getCheckedCount(ProductStatus.WAIT) +  adminService.getCheckedCount(ProductStatus.RE_WAIT);

            List<ProductStatus> statuses = new ArrayList<>();
            if(status == ProductStatus.PASS)
                statuses = List.of(ProductStatus.PASS, ProductStatus.RE_PASS);
            else if(status == ProductStatus.WAIT)
                statuses = List.of(ProductStatus.WAIT, ProductStatus.RE_WAIT);
            else if(status == ProductStatus.REJECT)
                statuses = List.of(ProductStatus.REJECT,  ProductStatus.RE_REJECT);
            else if(status == null)
                statuses = null;

            final Page<ProductEntity> products = adminService.getAllProducts(search, statuses, page);
            final Long productCnt = products.getTotalElements();
            final List<ProductManagementResponseDTO.ProductDTO> productList = adminService.getProductList(products);

            final ProductManagementResponseDTO management = ProductManagementResponseDTO.builder()
                    .passCnt(productPassCnt)
                    .waitCnt(productWaitCnt)
                    .productCnt(productCnt)
                    .products(productList)
                    .build();

            return ResponseEntity.ok().body(management);
        } catch(Exception e) {
            ResponseDTO resDTO = ResponseDTO.builder().error(e.getMessage()).build();
            return ResponseEntity.badRequest().body(resDTO);
        }
    }

    @PatchMapping("/products/{id}")
    public ResponseEntity<?> setProductInfo(@AuthenticationPrincipal UserEntity userInfo,
                                            @PathVariable("id") UUID productId,
                                            @RequestBody PlayTypeRequestDTO dto) {
        try {
            adminService.checkAuth(userInfo);

            adminService.updateProduct(productId, dto);

            return ResponseEntity.ok().body(true);
        } catch(Exception e) {
            ResponseDTO resDTO = ResponseDTO.builder().error(e.getMessage()).build();
            return ResponseEntity.badRequest().body(resDTO);
        }
    }

    @GetMapping(value = "/download/{id}", produces = "application/json; charset=UTF-8")
    public ResponseEntity<?> scriptDownload(@AuthenticationPrincipal UserEntity userInfo,
                                            @PathVariable("id") UUID productId) {
        try {
            adminService.checkAuth(userInfo);

            // 거절 일주일 뒤부터 작품 삭제 - updateAt, REJECT 조합으로 결정
            final ProductEntity product = adminService.getProduct(productId);

            final String filePath = product.getFilePath();
            final String title = product.getTitle();

            byte[] fileData = adminService.downloadFile(filePath);
            String encodedFilename = URLEncoder.encode(title, "UTF-8");

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF) // PDF 파일 형식으로 설정
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFilename)
                    .body(fileData);
        } catch (Exception e) {
            ResponseDTO resDTO = ResponseDTO.builder().error(e.getMessage()).build();
            return ResponseEntity.badRequest().body(resDTO);
        }
    }

    @GetMapping("/orders")
    public ResponseEntity<?> orderManage(@AuthenticationPrincipal UserEntity userInfo,
                                         @RequestParam(value = "page", defaultValue = "0") int page,
                                         @RequestParam(value = "search", required = false, defaultValue = "") String search) {
        try {
            adminService.checkAuth(userInfo);

            OrderManagementResponseDTO orders;

            if (search == null || search.trim().isEmpty()) {
                orders = adminService.getAllOrders(page);
            } else {
                orders = adminService.getAllOrderItems(search, page);
            }

            final OrderManagementResponseDTO management = OrderManagementResponseDTO.builder()
                    .orderCnt(orders.getOrderCnt())
                    .orders(orders.getOrders())
                    .build();

            return ResponseEntity.ok().body(management);
        } catch(Exception e) {
            ResponseDTO resDTO = ResponseDTO.builder().error(e.getMessage()).build();
            return ResponseEntity.badRequest().body(resDTO);
        }
    }

    @PatchMapping("/products/title")
    public ResponseEntity<?> updateTitle(@AuthenticationPrincipal UserEntity userInfo,
                                         @RequestBody UpdateTitleRequestDTO dto) {
        try {
            adminService.checkAuth(userInfo);
            adminService.updateTitle(dto.getProductId(), dto.getTitle());

            return ResponseEntity.ok().body(true);
        } catch(Exception e) {
            ResponseDTO resDTO = ResponseDTO.builder().error(e.getMessage()).build();
            return ResponseEntity.badRequest().body(resDTO);
        }
    }

    @PatchMapping("/products/writer")
    public ResponseEntity<?> updateWriter(@AuthenticationPrincipal UserEntity userInfo,
                                          @RequestBody UpdateWriterRequestDTO dto) {
        try {
            adminService.checkAuth(userInfo);
            adminService.updateWriter(dto.getProductId(), dto.getWriter());

            return ResponseEntity.ok().body(true);
        } catch(Exception e) {
            ResponseDTO resDTO = ResponseDTO.builder().error(e.getMessage()).build();
            return ResponseEntity.badRequest().body(resDTO);
        }
    }

    @GetMapping("/statistics")
    public ResponseEntity<?> getStatistics(@AuthenticationPrincipal UserEntity userInfo) {
        try {
            adminService.checkAuth(userInfo);

            final StatisticsResponseDTO resDTO = adminService.getStatistics();

            return ResponseEntity.ok().body(resDTO);
        } catch(Exception e) {
            ResponseDTO resDTO = ResponseDTO.builder().error(e.getMessage()).build();
            return ResponseEntity.badRequest().body(resDTO);
        }
    }
}
