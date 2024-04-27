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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

@RequiredArgsConstructor
@Controller
@Slf4j
public class RegisterController {
    private final ProductService service;

    @PostMapping("/fileSystem")
    public ResponseEntity<?> uploadImageToFileSystem(@RequestParam("image") MultipartFile file) throws IOException {
        String uploadImage = service.uploadImageToFileSystem(file);
        return ResponseEntity.status(HttpStatus.OK)
                .body(uploadImage);
    }
}
