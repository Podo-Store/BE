package PodoeMarket.podoemarket.controller;

import PodoeMarket.podoemarket.Utils.ValidUser;
import PodoeMarket.podoemarket.dto.ResponseDTO;
import PodoeMarket.podoemarket.dto.UserDTO;
import PodoeMarket.podoemarket.entity.UserEntity;
import PodoeMarket.podoemarket.repository.UserRepository;
import PodoeMarket.podoemarket.service.MailSendService;
import PodoeMarket.podoemarket.service.MypageService;
import PodoeMarket.podoemarket.service.UserService;
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

@RequiredArgsConstructor
@Controller
@Slf4j
@RequestMapping("/profile")
public class MypageController {
    private final MypageService mypageService;
    private final MailSendService mailService;
    private final PasswordEncoder pwdEncoder = new BCryptPasswordEncoder();

    @PostMapping("/checkPw")
    public ResponseEntity<?> checkPassword(@RequestBody UserDTO dto){
        try{
            // dto.getId는 프론트에서 토큰에 담겨 있는 id를 받아 확인해야 함
            boolean confirm = mypageService.checkUser(dto.getId(), dto.getPassword(), pwdEncoder);
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

    @PostMapping("/account")
    public ResponseEntity<?> updateAccount(@AuthenticationPrincipal UserEntity userInfo, @RequestBody UserDTO dto) {
        try{
            log.info("start account update");

            if(!ValidUser.isValidUser(dto)) {
                String msg = "유효성 검사 통과 실패";
                return ResponseEntity.badRequest().body(msg);
            }

            // 인증번호 확인x
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

            return ResponseEntity.ok().body(true);
        } catch(Exception e) {
            ResponseDTO resDTO = ResponseDTO.builder().error(e.getMessage()).build();
            return ResponseEntity.badRequest().body(resDTO);
        }
    }
}
