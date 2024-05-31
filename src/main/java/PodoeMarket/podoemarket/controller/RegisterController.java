package PodoeMarket.podoemarket.controller;

import PodoeMarket.podoemarket.dto.ResponseDTO;
import PodoeMarket.podoemarket.entity.ProductEntity;
import PodoeMarket.podoemarket.entity.UserEntity;
import PodoeMarket.podoemarket.service.MypageService;
import PodoeMarket.podoemarket.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@RequiredArgsConstructor
@Controller
@Slf4j
public class RegisterController {
    private final ProductService productService;
    private final MypageService mypageService;

   @Value("${UPLOAD_LOCATION}")
    private String uploadLoc;

    @PostMapping("/register")
    public ResponseEntity<?> scriptRegister(@AuthenticationPrincipal UserEntity userInfo, @RequestParam("script") MultipartFile file) {
        try{
            UserEntity user = mypageService.originalUser(userInfo.getId());

            // 파일 저장 경로 파싱
            String currentDate = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
            File uploadDir = new File(uploadLoc + File.separator + currentDate);

            // 폴더가 없으면 생성
            if (!uploadDir.exists()) {
                uploadDir.mkdirs();
            }

            File dest = new File(uploadDir.getAbsolutePath() + File.separator + file.getOriginalFilename());

            ProductEntity script = ProductEntity.builder()
                    .title(FilenameUtils.getBaseName(file.getOriginalFilename()))
                    .writer(user.getNickname())
                    .type(file.getContentType())
                    .filePath(dest.getPath())
                    .user(userInfo)
                    .build();

            file.transferTo(dest);
            productService.register(script);

            return ResponseEntity.ok().body(true);
        } catch(Exception e) {
            ResponseDTO resDTO = ResponseDTO.builder().error(e.getMessage()).build();
            return ResponseEntity.badRequest().body(resDTO);
        }
    }
}
