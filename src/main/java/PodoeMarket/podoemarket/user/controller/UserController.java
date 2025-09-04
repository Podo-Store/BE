package PodoeMarket.podoemarket.user.controller;

import PodoeMarket.podoemarket.user.dto.request.*;
import PodoeMarket.podoemarket.common.dto.ResponseDTO;
import PodoeMarket.podoemarket.common.entity.UserEntity;
import PodoeMarket.podoemarket.service.MailSendService;
import PodoeMarket.podoemarket.user.dto.response.FindPasswordResponseDTO;
import PodoeMarket.podoemarket.user.dto.response.FindUserIdResponseDTO;
import PodoeMarket.podoemarket.user.dto.response.TokenCreateResponseDTO;
import PodoeMarket.podoemarket.user.dto.response.TokenResponseDTO;
import PodoeMarket.podoemarket.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@Slf4j
@RequestMapping("/auth")
public class UserController {
    private final UserService userService;
    private final MailSendService mailService;

    @PostMapping("/checkUserId")
    public ResponseEntity<?> duplicateUserId(@RequestBody UserIdCheckRequestDTO dto) {
        try {
            boolean result = userService.validateUserId(dto);

            if(!result) {
                String errorMessage = dto.getCheck() ? "이미 존재하는 아이디" : "존재하지 않는 아이디";

                ResponseDTO resDTO = ResponseDTO.builder()
                        .error(errorMessage)
                        .build();

                return ResponseEntity.badRequest().body(resDTO);
            }

            return ResponseEntity.ok().body(true);
        } catch(Exception e) {
            ResponseDTO resDTO = ResponseDTO.builder().error(e.getMessage()).build();
            return ResponseEntity.badRequest().body(resDTO);
        }
    }

    @PostMapping("/equalPw")
    public ResponseEntity<?> equalPassword(@RequestBody PwCheckRequestDTO dto) {
        try{
            if(!dto.getPassword().equals(dto.getConfirmPassword())){
                ResponseDTO resDTO = ResponseDTO.builder()
                        .error("비밀번호 불일치")
                        .build();

                return ResponseEntity.badRequest().body(resDTO);
            }

            return ResponseEntity.ok().body(true);
        } catch(Exception e) {
            ResponseDTO resDTO = ResponseDTO.builder().error(e.getMessage()).build();
            return ResponseEntity.badRequest().body(resDTO);
        }
    }

    @PostMapping("/checkNickname")
    public ResponseEntity<?> duplicateNickname(@RequestBody NicknameCheckRequestDTO dto){
        try {
            userService.checkNickname(dto.getNickname());

            return ResponseEntity.ok().body(true);
        } catch(Exception e) {
            ResponseDTO resDTO = ResponseDTO.builder().error(e.getMessage()).build();
            return ResponseEntity.badRequest().body(resDTO);
        }
    }

    @PostMapping ("/mailSend")
    public ResponseEntity<?> mailSend(@RequestBody @Valid EmailSendRequestDTO dto){
        try {
            userService.validateAndSendEmail(dto.getEmail());

            return ResponseEntity.ok().body(true);
        } catch(Exception e) {
            ResponseDTO resDTO = ResponseDTO.builder().error(e.getMessage()).build();
            return ResponseEntity.badRequest().body(resDTO);
        }
    }

    @PostMapping("/mailauthCheck")
    public ResponseEntity<?> authCheck(@RequestBody @Valid EmailCheckRequestDTO dto){
        try{
            boolean Checked = mailService.CheckAuthNum(dto.getEmail(),dto.getAuthNum());

            if(Checked)
                return ResponseEntity.ok().body(true);
            else
                return ResponseEntity.badRequest().body(false);
        } catch(Exception e) {
            ResponseDTO resDTO = ResponseDTO.builder().error(e.getMessage()).build();
            return ResponseEntity.badRequest().body(resDTO);
        }
    }

    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@RequestBody SignUpRequestDTO dto) {
        try{
            // 인증번호 확인
            if (!mailService.CheckAuthNum(dto.getEmail(), dto.getAuthNum())) {
                ResponseDTO resDTO = ResponseDTO.builder()
                        .error("이메일 인증 실패")
                        .build();

                return ResponseEntity.badRequest().body(resDTO);
            }

            final TokenCreateResponseDTO user = userService.create(dto);

            return ResponseEntity.ok().body(user);
        } catch(Exception e) {
            ResponseDTO resDTO = ResponseDTO.builder().error(e.getMessage()).build();
            return ResponseEntity.badRequest().body(resDTO);
        }
    }

    @PostMapping("/signin")
    public ResponseEntity<?> authenticate(@RequestBody SignInRequestDTO dto) {
        try{
            final TokenCreateResponseDTO user = userService.getCredentialsSignIn(dto.getUserId(), dto.getPassword());

            return ResponseEntity.ok().body(user);
        } catch(Exception e) {
            ResponseDTO resDTO = ResponseDTO.builder().error(e.getMessage()).build();
            return ResponseEntity.badRequest().body(resDTO);
        }
    }

    @PostMapping("/find/mailSend")
    public ResponseEntity<?> findMailSend(@RequestBody EmailRequestDTO dto) {
        try {
            final String authNum = userService.findUserInfo(dto);

            return ResponseEntity.ok().body(authNum);
        } catch(Exception e) {
            ResponseDTO resDTO = ResponseDTO.builder().error(e.getMessage()).build();
            return ResponseEntity.badRequest().body(resDTO);
        }
    }

    @PostMapping("/findUserId")
    public ResponseEntity<?> findUserId(@RequestBody FindUserIdRequestDTO dto) {
        try{
            // 인증번호 확인
            if (!mailService.CheckAuthNum(dto.getEmail(), dto.getAuthNum())) {
                ResponseDTO resDTO = ResponseDTO.builder()
                        .error("이메일 인증 실패")
                        .build();

                return ResponseEntity.badRequest().body(resDTO);
            }

            final FindUserIdResponseDTO resDTO = userService.findUserId(dto);

            return ResponseEntity.ok().body(resDTO);
        } catch(Exception e) {
            ResponseDTO resDTO = ResponseDTO.builder().error(e.getMessage()).build();
            return ResponseEntity.badRequest().body(resDTO);
        }
    }

    @PostMapping("/findPassword")
    public ResponseEntity<?> findPassword(@RequestBody FindPasswordRequestDTO dto) {
        try{
            // 인증번호 확인
            if (!mailService.CheckAuthNum(dto.getEmail(), dto.getAuthNum())) {
                ResponseDTO resDTO = ResponseDTO.builder()
                        .error("이메일 인증 실패")
                        .build();

                return ResponseEntity.badRequest().body(resDTO);
            }

            final FindPasswordResponseDTO resDTO = userService.findPassword(dto);

            return ResponseEntity.ok().body(resDTO);
        } catch(Exception e) {
            ResponseDTO resDTO = ResponseDTO.builder().error(e.getMessage()).build();
            return ResponseEntity.badRequest().body(resDTO);
        }
    }

    @PostMapping("/resetPassword")
    public ResponseEntity<?> restPassword(@AuthenticationPrincipal UserEntity userInfo, @RequestBody PwCheckRequestDTO dto) {
        try{
            userService.updatePassword(userInfo.getId(), dto);

            return ResponseEntity.ok().body(true);
        } catch(Exception e) {
            ResponseDTO resDTO = ResponseDTO.builder().error(e.getMessage()).build();
            return ResponseEntity.badRequest().body(resDTO);
        }
    }

    // accessToken 재발급
    @PostMapping("/newToken")
    public ResponseEntity<?> createNewToken(HttpServletRequest request){
        try {
            String token = request.getHeader("Authorization").substring(7);

            final TokenResponseDTO resDTO = userService.createNewToken(token);

            return ResponseEntity.ok().body(resDTO);
        }catch (Exception e){
            log.error("/auth/newToken 실행 중 예외 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("newToken fail");
        }
    }
}
