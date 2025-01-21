package PodoeMarket.podoemarket.controller;

import PodoeMarket.podoemarket.dto.response.ProductManagementDTO;
import PodoeMarket.podoemarket.dto.response.ResponseDTO;
import PodoeMarket.podoemarket.entity.ProductEntity;
import PodoeMarket.podoemarket.entity.UserEntity;
import PodoeMarket.podoemarket.entity.type.ProductStatus;
import PodoeMarket.podoemarket.service.AdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequiredArgsConstructor
@Slf4j
@RequestMapping("/admin")
@RestController
public class AdminController {
    private final AdminService adminService;

    @GetMapping("/product")
    public ResponseEntity<?> productManage(@AuthenticationPrincipal UserEntity userInfo,
                                           @RequestParam(value = "page", defaultValue = "0") int page,
                                           @RequestParam(value = "search", required = false, defaultValue = "") String search,
                                           @RequestParam(value = "status", required = false) ProductStatus status) {
        try {
//            if (!userInfo.isAuth()) {
//                ResponseDTO resDTO = ResponseDTO.builder()
//                        .error("어드민이 아닙니다.")
//                        .build();
//
//                return ResponseEntity.badRequest().body(resDTO);
//            }

            final Long productPassCnt = adminService.getCheckedCount(ProductStatus.PASS);
            final Long productWaitCnt = adminService.getCheckedCount(ProductStatus.WAIT);

            final Page<ProductEntity> products = adminService.getAllProducts(search, status, page);
            final Long productCnt = products.getTotalElements();
            final List<ProductManagementDTO.ProductDTO> productList = adminService.getProductList(products);

            final ProductManagementDTO management = ProductManagementDTO.builder()
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
}
