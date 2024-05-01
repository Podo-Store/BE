package PodoeMarket.podoemarket.controller;

import PodoeMarket.podoemarket.dto.ResponseDTO;
import PodoeMarket.podoemarket.entity.ProductEntity;
import PodoeMarket.podoemarket.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.Objects;

@RequiredArgsConstructor
@Controller
@Slf4j
public class RegisterController {
    private final ProductService productService;

    @PostMapping("/register")
    public ResponseEntity<?> scriptRegister(@RequestParam("script") MultipartFile file) {
        try{
            String filePath = file.getOriginalFilename();

            ProductEntity script = ProductEntity.builder()
                    .type(file.getContentType())
                    .filePath(filePath)
                    .build();

            // 파일 경로
            file.transferTo(new File(Objects.requireNonNull(filePath)));
            productService.register(script);

            return ResponseEntity.ok().body(true);
        } catch(Exception e) {
            ResponseDTO resDTO = ResponseDTO.builder().error(e.getMessage()).build();
            return ResponseEntity.badRequest().body(resDTO);
        }
    }
}
