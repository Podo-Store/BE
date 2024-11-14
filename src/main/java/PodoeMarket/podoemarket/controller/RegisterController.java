package PodoeMarket.podoemarket.controller;

import PodoeMarket.podoemarket.dto.response.ResponseDTO;
import PodoeMarket.podoemarket.entity.ProductEntity;
import PodoeMarket.podoemarket.entity.UserEntity;
import PodoeMarket.podoemarket.service.MailSendService;
import PodoeMarket.podoemarket.service.MypageService;
import PodoeMarket.podoemarket.service.RegisterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.apache.commons.io.FilenameUtils;

@RequiredArgsConstructor
@Controller
@Slf4j
public class RegisterController {
    private final RegisterService registerService;
    private final MypageService mypageService;
    private final MailSendService mailSendService;

    @PostMapping("/register")
    public ResponseEntity<?> scriptRegister(@AuthenticationPrincipal UserEntity userInfo, @RequestParam("script") MultipartFile[] files) {
        try{
            UserEntity user = mypageService.originalUser(userInfo.getId());

            String filePath = registerService.uploadScript(files, userInfo.getNickname());

            ProductEntity script = ProductEntity.builder()
                    .title(FilenameUtils.getBaseName(files[0].getOriginalFilename()))
                    .writer(user.getNickname())
                    .filePath(filePath)
                    .user(userInfo)
                    .build();

            registerService.register(script);

            mailSendService.joinRegisterEmail(user.getEmail());

            return ResponseEntity.ok().body(true);
        } catch(Exception e) {
            ResponseDTO resDTO = ResponseDTO.builder().error(e.getMessage()).build();
            return ResponseEntity.badRequest().body(resDTO);
        }
    }
}


