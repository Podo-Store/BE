package PodoeMarket.podoemarket.controller;

import PodoeMarket.podoemarket.dto.ProductDTO;
import PodoeMarket.podoemarket.dto.ResponseDTO;
import PodoeMarket.podoemarket.entity.FileEntity;
import PodoeMarket.podoemarket.entity.ProductEntity;
import PodoeMarket.podoemarket.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

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
            ResponseDTO resDTO = ResponseDTO.builder().error(e.getMessage()).build();
            return ResponseEntity.badRequest().body(resDTO);
        }
    }

    @PostMapping("/upload")
    public ResponseEntity upload(@RequestPart MultipartFile file) {
        String originalFileName = file.getOriginalFilename();
//        String filePath = "upload/dir/" + originalFileName;
//        String filePath = originalFileName;

        try {
//            file.transferTo(new File(filePath));
//            FileEntity savedFile = service.uploadFile(originalFileName, filePath);

            file.transferTo(new File(originalFileName));
            FileEntity savedFile = service.uploadFile(originalFileName, originalFileName);

            return ResponseEntity.ok().body(savedFile);
        } catch (Exception e) {
            ResponseDTO resDTO = ResponseDTO.builder().error(e.getMessage()).build();
            return ResponseEntity.badRequest().body(resDTO);
        }
    }
}
