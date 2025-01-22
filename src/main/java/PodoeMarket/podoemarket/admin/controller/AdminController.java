package PodoeMarket.podoemarket.admin.controller;

import PodoeMarket.podoemarket.admin.dto.request.PlayTypeRequestDTO;
import PodoeMarket.podoemarket.admin.service.AdminService;
import PodoeMarket.podoemarket.admin.dto.response.ProductManagementResponseDTO;
import PodoeMarket.podoemarket.dto.response.ResponseDTO;
import PodoeMarket.podoemarket.entity.ProductEntity;
import PodoeMarket.podoemarket.entity.UserEntity;
import PodoeMarket.podoemarket.entity.type.ProductStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

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

            final Long productPassCnt = adminService.getCheckedCount(ProductStatus.PASS);
            final Long productWaitCnt = adminService.getCheckedCount(ProductStatus.WAIT);

            final Page<ProductEntity> products = adminService.getAllProducts(search, status, page);
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

    @PatchMapping("/{id}")
    public ResponseEntity<?> setProductInfo(@AuthenticationPrincipal UserEntity userInfo,
                                            @PathVariable("id") UUID productId,
                                            @RequestBody PlayTypeRequestDTO dto) {
        try {
            adminService.checkAuth(userInfo);

            ProductEntity product = adminService.getProduct(productId);

            if (dto.getPlayType() != null)
                product.setPlayType(dto.getPlayType());

            if (dto.getProductStatus() != null)
                product.setChecked(dto.getProductStatus());

            adminService.updateProduct(product);

            return ResponseEntity.ok().body(true);
        } catch(Exception e) {
            ResponseDTO resDTO = ResponseDTO.builder().error(e.getMessage()).build();
            return ResponseEntity.badRequest().body(resDTO);
        }
    }
}
