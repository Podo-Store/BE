package PodoeMarket.podoemarket.register.controller;

import PodoeMarket.podoemarket.dto.response.ResponseDTO;
import PodoeMarket.podoemarket.common.entity.ProductEntity;
import PodoeMarket.podoemarket.common.entity.UserEntity;
import PodoeMarket.podoemarket.common.entity.type.ProductStatus;
import PodoeMarket.podoemarket.register.service.RegisterService;
import PodoeMarket.podoemarket.service.MailSendService;
import PodoeMarket.podoemarket.profile.service.MypageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.apache.commons.io.FilenameUtils;

import java.text.Normalizer;

@RequiredArgsConstructor
@RestController
@Slf4j
public class RegisterController {
    private final RegisterService registerService;
    private final MailSendService mailSendService;

    @PostMapping(value = "/register", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> scriptRegister(@AuthenticationPrincipal UserEntity userInfo, @RequestPart("script") MultipartFile[] files) {
        try{
            UserEntity user = registerService.originalUser(userInfo.getId());

            String filePath = registerService.uploadScript(files, userInfo.getNickname());
            String normalizedTitle = Normalizer.normalize(FilenameUtils.getBaseName(files[0].getOriginalFilename()), Normalizer.Form.NFKC);

            ProductEntity script = ProductEntity.builder()
                    .title(normalizedTitle)
                    .writer(user.getNickname())
                    .filePath(filePath)
                    .checked(ProductStatus.WAIT)
                    .user(userInfo)
                    .build();

            registerService.register(script);

            mailSendService.joinRegisterEmail(user.getEmail(), normalizedTitle);

            return ResponseEntity.ok().body(true);
        } catch(Exception e) {
            ResponseDTO resDTO = ResponseDTO.builder().error(e.getMessage()).build();
            return ResponseEntity.badRequest().body(resDTO);
        }
    }
}


