package PodoeMarket.podoemarket.controller;

import PodoeMarket.podoemarket.dto.UserDTO;
import PodoeMarket.podoemarket.entity.UserEntity;
import PodoeMarket.podoemarket.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.regex.Pattern;

@RequiredArgsConstructor
@Controller
@Slf4j
@RequestMapping("/auth")
public class UserController {

    private final UserService service;
    private final PasswordEncoder pwdEncoder = new BCryptPasswordEncoder();

    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@RequestBody UserDTO dto) {
        try{
            log.info("Start signup");

            // 유저 유효성 검사
            if (!isValidUser(dto)){
                return ResponseEntity.badRequest().body(false);
            }

            UserEntity user = UserEntity.builder()
                    .email(dto.getEmail())
                    .password(pwdEncoder.encode(dto.getPassword()))
                    .phoneNumber(dto.getPhoneNumber())
                    .nickname(dto.getNickname())
                    .auth(false)
                    .build();

            service.create(user);

            return ResponseEntity.ok().body(true);
        } catch(Exception e) {
            return ResponseEntity.badRequest().body(false);
        }
    }
    
    // 유효성 검사
    private boolean isValidUser(UserDTO userDTO){ // 이메일, 비밀번호, 전화번호
        String regx_pwd = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z]).{5,}$"; // 숫자 최소 1개, 대소문자 최소 1개, 최소 길이 5개 이상
        String regx_email = "^[\\w-]+(\\.[\\w-]+)*@[\\w-]+(\\.[\\w-]+)*(\\.[a-zA-Z]{2,})$";
        String regx_tel = "^01(?:0|1|[6-9])(?:\\d{3}|\\d{4})\\d{4}$";

        if(userDTO.getEmail() == null || userDTO.getEmail().isBlank()){ //이메일이 null이거나 빈 값일때
            log.warn("email is null or empty");
            return false;
        }else if(userDTO.getPassword() == null || userDTO.getPassword().isBlank()){ //password가 null이거나 빈 값일때
            log.warn("password is null or empty");
            return false;
        }else if(userDTO.getPassword().length() < 4 || userDTO.getPassword().length() > 12) { // password의 길이는 4 초과, 12 미만
            log.warn("password is too long or short");
            return false;
        }else if(!Pattern.matches(regx_pwd, userDTO.getPassword())){
            log.warn("password is not fit in the rule");
            return false;
        }else if(!Pattern.matches(regx_email, userDTO.getEmail())){
            log.warn("email is not fit in the rule");
            return false;
        }else if(!Pattern.matches(regx_tel, userDTO.getPhoneNumber())){
            log.warn("phonenumber is not fit in the rule");
            return false;
        }else {
            log.info("user valid checked");
            return true;
        }
    }
}
