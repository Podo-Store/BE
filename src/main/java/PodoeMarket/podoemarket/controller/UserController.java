package PodoeMarket.podoemarket.controller;

import PodoeMarket.podoemarket.Utils.ValidUser;
import PodoeMarket.podoemarket.dto.EmailCheckDTO;
import PodoeMarket.podoemarket.dto.EmailRequestDTO;
import PodoeMarket.podoemarket.dto.ResponseDTO;
import PodoeMarket.podoemarket.dto.UserDTO;
import PodoeMarket.podoemarket.entity.UserEntity;
import PodoeMarket.podoemarket.security.TokenProvider;
import PodoeMarket.podoemarket.service.MailSendService;
import PodoeMarket.podoemarket.service.RedisUtil;
import PodoeMarket.podoemarket.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@Controller
@Slf4j
@RequestMapping("/auth")
public class UserController {
    private final UserService userService;
    private final TokenProvider tokenProvider;
    private final MailSendService mailService;
    private final RedisUtil redisUtil;
    private final PasswordEncoder pwdEncoder = new BCryptPasswordEncoder();

    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@RequestBody UserDTO dto) {
        try{
            log.info("Start signup");

            // 유저 유효성 검사
            if(!ValidUser.isValidUser(dto)){
                String msg = "유효성 검사 통과 실패";
                return ResponseEntity.badRequest().body(msg);
            }

            // 인증번호 확인
            if (!mailService.CheckAuthNum(dto.getEmail(), dto.getAuthNum())) {
                String msg = "이메일 인증 실패";
                return ResponseEntity.badRequest().body(msg);
            }

            UserEntity user = UserEntity.builder()
                    .userId(dto.getUserId())
                    .password(pwdEncoder.encode(dto.getPassword()))
                    .nickname(dto.getNickname())
                    .email(dto.getEmail())
                    .auth(false)
                    .build();

            userService.create(user);
            redisUtil.deleteData(dto.getAuthNum()); // 인증 번호 확인 후, redis 상에서 즉시 삭제

            return ResponseEntity.ok().body(true);
        } catch(Exception e) {
            return ResponseEntity.badRequest().body(false);
        }
    }

    @GetMapping("/checkuserId")
    public ResponseEntity<?> duplicateUserId(@RequestParam String userId) {
        try {
            log.info("check userId duplication");

            boolean isexists = userService.checkUserId(userId);

            if(isexists) {
                return ResponseEntity.badRequest().body(false);
            } else{
                return ResponseEntity.ok().body(true);
            }
        } catch(Exception e) {
            return ResponseEntity.badRequest().body(false);
        }
    }

    @GetMapping("/checkemail")
    public ResponseEntity<?> duplicateEmail(@RequestParam String email) {
        try {
            log.info("check email duplication");

            boolean isexists = userService.checkEmail(email);

            if(isexists) {
                return ResponseEntity.badRequest().body(false);
            } else{
                return ResponseEntity.ok().body(true);
            }
        } catch(Exception e) {
            return ResponseEntity.badRequest().body(false);
        }
    }

    @GetMapping("/checknickname")
    public ResponseEntity<?> duplicateNickname(@RequestParam String nickname){
        try {
            log.info("check nickname duplication");

            boolean isexists = userService.checkNickname(nickname);

            if(isexists) {
                return ResponseEntity.badRequest().body(false);
            } else{
                return ResponseEntity.ok().body(true);
            }
        } catch(Exception e) {
            return ResponseEntity.badRequest().body(false);
        }
    }

    @PostMapping ("/mailSend")
    public ResponseEntity<?> mailSend(@RequestBody @Valid EmailRequestDTO emailDTO){
        try {
            String email = emailDTO.getEmail();

            System.out.println("이메일 인증 요청이 들어옴");
            System.out.println("이메일 인증 이메일 :" + email);

            return ResponseEntity.ok().body(mailService.joinEmail(email));
        }catch (Exception e){
            return ResponseEntity.badRequest().body(false);
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
        }catch (Exception e){
            return ResponseEntity.badRequest().body(false);
        }
    }

    @PostMapping("/signin")
    public ResponseEntity<?> authenticate(@RequestBody UserDTO dto) {
        try{
            log.info("Start signin");

            UserEntity user = userService.getByCredentials(dto.getUserId(), dto.getPassword(), pwdEncoder);
            log.info("user: {}", user);

            if(user.getId() != null) {
                final String accessToken = tokenProvider.createAccessToken(user);
                final String refreshToken = tokenProvider.createRefreshToken(user);
                log.info("accessToken value: {}", accessToken);
                log.info("finish creating token");

                final UserDTO resUserDTO = UserDTO.builder()
                        .id(user.getId())
                        .userId(user.getUserId())
                        .email(user.getEmail())
                        .password(user.getPassword())
                        .nickname(user.getNickname())
                        .accessToken(accessToken)
                        .refreshToken(refreshToken)
                        .build();

                return ResponseEntity.ok().body(resUserDTO);
            } else {
                // 아이디, 비번으로 찾은 유저 없음 = 로그인 실패
                ResponseDTO resDTO = ResponseDTO.builder()
                        .error(user.getNickname())
                        .build();

                return ResponseEntity.badRequest().body(resDTO);
            }
        }catch (Exception e){
            log.error("exception in /auth/signin", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("signin fail");
        }
    }
}
