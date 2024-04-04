package PodoeMarket.podoemarket.controller;

import PodoeMarket.podoemarket.dto.ProductDTO;
import PodoeMarket.podoemarket.entity.ProductEntity;
import PodoeMarket.podoemarket.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@RequiredArgsConstructor
@Controller
@Slf4j
public class RegisterController {
    private final ProductService service;

    @PostMapping("/register")
    public ResponseEntity<?> registerProduct(@RequestBody ProductDTO dto) {
        try {
            log.info("Start register");

            ProductEntity product = ProductEntity.builder()
                    .file(dto.getFile())
                    .build();

            service.create(product);

            log.info(String.valueOf(product));

            return ResponseEntity.ok().body(true);
        } catch(Exception e) {
            return ResponseEntity.badRequest().body(false);
        }
    }
}
