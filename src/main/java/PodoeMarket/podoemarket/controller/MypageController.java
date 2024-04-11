package PodoeMarket.podoemarket.controller;

import PodoeMarket.podoemarket.Utils.ValidUser;
import PodoeMarket.podoemarket.dto.ResponseDTO;
import PodoeMarket.podoemarket.dto.UserDTO;
import PodoeMarket.podoemarket.entity.UserEntity;
import PodoeMarket.podoemarket.service.MypageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@RequiredArgsConstructor
@Controller
@Slf4j
@RequestMapping("/profile")
public class MypageController {
    private final MypageService service;

    @PostMapping("/account")
    public ResponseEntity<?> updateAccount(@AuthenticationPrincipal UserEntity userInfo, @RequestBody UserDTO dto) {
        try{
            log.info("start account update");

            if(!ValidUser.isValidUser(dto)) {
                String msg = "유효성 검사 통과 실패";
                return ResponseEntity.badRequest().body(msg);
            }

            UserEntity user = UserEntity.builder()
                    .password(dto.getPassword())
                    .nickname(dto.getNickname())
                    .phoneNumber(dto.getPhoneNumber())
                    .email(dto.getEmail())
                    .build();

            // token 값 변경 가능성 있음
            service.update(userInfo.getId(), user);

            return ResponseEntity.ok().body(true);
        } catch(Exception e) {
            ResponseDTO resDTO = ResponseDTO.builder().error(e.getMessage()).build();
            return ResponseEntity.badRequest().body(resDTO);
        }
    }
}
