package PodoeMarket.podoemarket.controller;

import PodoeMarket.podoemarket.dto.UserDTO;
import PodoeMarket.podoemarket.entity.UserEntity;
import PodoeMarket.podoemarket.service.UserService;
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
@RequestMapping("/auth")
public class UserController {

    private final UserService service;
    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@RequestBody UserDTO dto) {
        try{
            // 유저 유효성 검사
//            String validCheck = isValidUser(dto);
//            if (!validCheck.equals("checked")){
//                return ResponseEntity.badRequest().body(false);
//            }

            UserEntity user = UserEntity.builder()
                    .email(dto.getEmail())
                    .password(dto.getPassword())
                    .phoneNumber(dto.getPhoneNumber())
                    .nickname(dto.getNickname())
                    .build();

            service.create(user);

            return ResponseEntity.ok().body(true);
        } catch(Exception e) {
            return ResponseEntity.badRequest().body(false);
        }
    }
}
