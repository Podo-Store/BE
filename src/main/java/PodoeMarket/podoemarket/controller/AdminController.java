package PodoeMarket.podoemarket.controller;

import PodoeMarket.podoemarket.dto.response.ProductManagementDTO;
import PodoeMarket.podoemarket.dto.response.ResponseDTO;
import PodoeMarket.podoemarket.entity.ProductEntity;
import PodoeMarket.podoemarket.entity.type.ProductStatus;
import PodoeMarket.podoemarket.service.AdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<?> productManage(@RequestParam(value = "page", defaultValue = "0") int page,
                                           @RequestParam(value = "search", required = false) String search) {
        try {
            final Long productPassCnt = adminService.getCheckedCount(ProductStatus.PASS);
            final Long productWaitCnt = adminService.getCheckedCount(ProductStatus.WAIT);

            final Page<ProductEntity> products = adminService.getAllProducts(search, page);
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
