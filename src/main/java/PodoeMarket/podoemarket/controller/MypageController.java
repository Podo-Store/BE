package PodoeMarket.podoemarket.controller;

import PodoeMarket.podoemarket.Utils.ValidUser;
import PodoeMarket.podoemarket.dto.EmailCheckDTO;
import PodoeMarket.podoemarket.dto.EmailRequestDTO;
import PodoeMarket.podoemarket.dto.ResponseDTO;
import PodoeMarket.podoemarket.dto.UserDTO;
import PodoeMarket.podoemarket.entity.UserEntity;
import PodoeMarket.podoemarket.repository.UserRepository;
import PodoeMarket.podoemarket.service.MailSendService;
import PodoeMarket.podoemarket.service.MypageService;
import PodoeMarket.podoemarket.service.RedisUtil;
import PodoeMarket.podoemarket.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Collections;
import java.util.Objects;

@RequiredArgsConstructor
@Controller
@Slf4j
@RequestMapping("/profile")
public class MypageController {
    private final MypageService mypageService;
    private final MailSendService mailService;
    private final UserService userService;
    private final RedisUtil redisUtil;
    private final PasswordEncoder pwdEncoder = new BCryptPasswordEncoder();

    @PostMapping("/confirm")
    public ResponseEntity<?> confirmPassword(@AuthenticationPrincipal UserEntity userInfo, @RequestBody UserDTO dto){
        try{
            boolean confirm = mypageService.checkUser(userInfo.getId(), dto.getPassword(), pwdEncoder);
            if(confirm)
                return ResponseEntity.ok().body(true);
            else{
                ResponseDTO resDTO = ResponseDTO.builder()
                        .error("비밀번호 불일치")
                        .build();

                return ResponseEntity.badRequest().body(resDTO);
            }
        } catch(Exception e) {
            ResponseDTO resDTO = ResponseDTO.builder().error(e.getMessage()).build();
            return ResponseEntity.badRequest().body(resDTO);
        }
    }

    @PostMapping("/checkPw")
    public ResponseEntity<?> checkPassword(@RequestBody UserDTO dto) {
        if(!ValidUser.isValidPw(dto.getPassword())){
            ResponseDTO resDTO = ResponseDTO.builder()
                    .error("비밀번호 유효성 검사 실패")
                    .build();

            return ResponseEntity.badRequest().body(resDTO);
        }

        return ResponseEntity.ok().body(true);
    }

    @PostMapping("/equalPw")
    public ResponseEntity<?> equalPassword(@RequestBody UserDTO dto) {
        if(!dto.getPassword().equals(dto.getConfirmPassword())){
            ResponseDTO resDTO = ResponseDTO.builder()
                    .error("비밀번호 불일치")
                    .build();

            return ResponseEntity.badRequest().body(resDTO);
        }

        return ResponseEntity.ok().body(true);
    }

    @PostMapping("/checkNickname")
    public ResponseEntity<?> checkNickname(@RequestBody UserDTO dto) {
        UserEntity originalUser = mypageService.originalUser(dto.getId());

        // 기존의 닉네임과 다를 경우
        if(!Objects.equals(originalUser.getNickname(), dto.getNickname())) {
            if(!ValidUser.isValidNickname(dto.getNickname())){
                ResponseDTO resDTO = ResponseDTO.builder()
                        .error("닉네임 유효성 검사 실패")
                        .build();

                return ResponseEntity.badRequest().body(resDTO);
            }

            if(userService.checkNickname(dto.getNickname())) {
                ResponseDTO resDTO = ResponseDTO.builder()
                        .error("닉네임 중복")
                        .build();

                return ResponseEntity.badRequest().body(resDTO);
            }
        }

        return ResponseEntity.ok().body(true);
    }

    @PostMapping ("/mailSend")
    public ResponseEntity<?> mailSend(@RequestBody @Valid EmailRequestDTO emailDTO){
        try {
            String email = emailDTO.getEmail();

            log.info("이메일 인증 요청이 들어옴");
            log.info("이메일 인증 이메일 :" + email);

            return ResponseEntity.ok().body(mailService.joinEmail(email));
        }catch(Exception e) {
            ResponseDTO resDTO = ResponseDTO.builder().error(e.getMessage()).build();
            return ResponseEntity.badRequest().body(resDTO);
        }
    }

    @PostMapping("/mailauthCheck")
    public ResponseEntity<?> AuthCheck(@RequestBody @Valid EmailCheckDTO emailCheckDTO){
        try{
            log.info("Start mailauthCheck");

            boolean Checked = mailService.CheckAuthNum(emailCheckDTO.getEmail(),emailCheckDTO.getAuthNum());

            if(Checked) {
                return ResponseEntity.ok().body(true);
            } else
                throw new NullPointerException("Null Exception");
        } catch(Exception e) {
            ResponseDTO resDTO = ResponseDTO.builder().error(e.getMessage()).build();
            return ResponseEntity.badRequest().body(resDTO);
        }
    }

    @PostMapping("/account")
    public ResponseEntity<?> updateAccount(@AuthenticationPrincipal UserEntity userInfo, @RequestBody UserDTO dto) {
        try{
            log.info("start account update");

            if(!ValidUser.isValidUser(dto)) {
                ResponseDTO resDTO = ResponseDTO.builder()
                        .error("유효성 검사 실패")
                        .build();

                return ResponseEntity.badRequest().body(resDTO);
            }

            if (!mailService.CheckAuthNum(dto.getEmail(), dto.getAuthNum())) {
                ResponseDTO resDTO = ResponseDTO.builder()
                        .error("이메일 인증 실패")
                        .build();

                return ResponseEntity.badRequest().body(resDTO);
            }

            UserEntity user = UserEntity.builder()
                    .userId(dto.getUserId())
                    .password(pwdEncoder.encode(dto.getPassword()))
                    .nickname(dto.getNickname())
                    .name(dto.getName())
                    .email(dto.getEmail())
                    .build();

            // token 값 변경 가능성 있음
            mypageService.update(userInfo.getId(), user);
            redisUtil.deleteData(dto.getAuthNum()); // 인증 번호 확인 후, redis 상에서 즉시 삭제

            return ResponseEntity.ok().body(true);
        } catch(Exception e) {
            ResponseDTO resDTO = ResponseDTO.builder().error(e.getMessage()).build();
            return ResponseEntity.badRequest().body(resDTO);
        }
    }
}
