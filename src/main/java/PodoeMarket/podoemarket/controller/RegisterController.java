package PodoeMarket.podoemarket.controller;

import PodoeMarket.podoemarket.dto.ResponseDTO;
import PodoeMarket.podoemarket.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RequiredArgsConstructor
@Controller
@Slf4j
public class RegisterController {
    private final ProductService productService;

    @PostMapping("/register")
    public ResponseEntity<?> scriptRegister(@RequestParam("image") MultipartFile file) {
        try{
            // 파일 저장
            productService.register(file);

            return ResponseEntity.ok().body(true);
        } catch(Exception e) {
            ResponseDTO resDTO = ResponseDTO.builder().error(e.getMessage()).build();
            return ResponseEntity.badRequest().body(resDTO);
        }
    }
}
