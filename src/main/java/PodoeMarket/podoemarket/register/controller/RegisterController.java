package PodoeMarket.podoemarket.register.controller;

import PodoeMarket.podoemarket.common.dto.ResponseDTO;
import PodoeMarket.podoemarket.common.entity.UserEntity;
import PodoeMarket.podoemarket.register.service.RegisterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RequiredArgsConstructor
@RestController
@Slf4j
public class RegisterController {
    private final RegisterService registerService;

    @PostMapping(value = "/register", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> scriptRegister(@AuthenticationPrincipal UserEntity userInfo, @RequestPart("script") MultipartFile[] files) {
        try {
            registerService.registerScript(userInfo, files);

            return ResponseEntity.ok().body(true);
        } catch (Exception e) {
            ResponseDTO resDTO = ResponseDTO.builder().error(e.getMessage()).build();
            return ResponseEntity.badRequest().body(resDTO);
        }
    }
}


