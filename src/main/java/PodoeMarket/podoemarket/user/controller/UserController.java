package PodoeMarket.podoemarket.user.controller;

import PodoeMarket.podoemarket.Utils.ValidCheck;
import PodoeMarket.podoemarket.common.config.jwt.JwtProperties;
import PodoeMarket.podoemarket.user.dto.request.EmailCheckRequestDTO;
import PodoeMarket.podoemarket.user.dto.request.EmailRequestDTO;
import PodoeMarket.podoemarket.dto.response.ResponseDTO;
import PodoeMarket.podoemarket.dto.UserDTO;
import PodoeMarket.podoemarket.common.entity.UserEntity;
import PodoeMarket.podoemarket.common.security.TokenProvider;
import PodoeMarket.podoemarket.service.MailSendService;
import PodoeMarket.podoemarket.service.VerificationService;
import PodoeMarket.podoemarket.user.dto.request.SignInRequestDTO;
import PodoeMarket.podoemarket.user.dto.response.SignInResponseDTO;
import PodoeMarket.podoemarket.user.dto.response.TokenResponseDTO;
import PodoeMarket.podoemarket.user.service.UserService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.*;

@RequiredArgsConstructor
@RestController
@Slf4j
@RequestMapping("/auth")
public class UserController {
    private final UserService userService;
    private final TokenProvider tokenProvider;
    private final MailSendService mailService;
    private final VerificationService verificationService;
    private final JwtProperties jwtProperties;
    private final PasswordEncoder pwdEncoder = new BCryptPasswordEncoder();

    @PostMapping("/checkUserId")
    public ResponseEntity<?> duplicateUserId(@RequestBody UserDTO dto) {
        try {
            if(dto.isCheck()) { // True : 회원가입, False : 비밀번호 찾기
                if(userService.checkUserId(dto.getUserId())) {
                    ResponseDTO resDTO = ResponseDTO.builder()
                            .error("이미 존재하는 아이디")
                            .build();

                    return ResponseEntity.badRequest().body(resDTO);
                }

                return ResponseEntity.ok().body(true);
            } else {
                if(!userService.checkUserId(dto.getUserId())) {
                    ResponseDTO resDTO = ResponseDTO.builder()
                            .error("존재하지 않는 아이디")
                            .build();

                    return ResponseEntity.badRequest().body(resDTO);
                }

                return ResponseEntity.ok().body(true);
            }
        } catch(Exception e) {
            ResponseDTO resDTO = ResponseDTO.builder().error(e.getMessage()).build();
            return ResponseEntity.badRequest().body(resDTO);
        }
    }

    @PostMapping("/equalPw")
    public ResponseEntity<?> equalPassword(@RequestBody UserDTO dto) {
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
    public ResponseEntity<?> duplicateNickname(@RequestBody UserDTO dto){
        try {
            if(userService.checkNickname(dto.getNickname())) {
                ResponseDTO resDTO = ResponseDTO.builder()
                        .error("닉네임 중복")
                        .build();

                return ResponseEntity.badRequest().body(resDTO);
            } else{
                return ResponseEntity.ok().body(true);
            }
        } catch(Exception e) {
            ResponseDTO resDTO = ResponseDTO.builder().error(e.getMessage()).build();
            return ResponseEntity.badRequest().body(resDTO);
        }
    }

    @PostMapping ("/mailSend")
    public ResponseEntity<?> mailSend(@RequestBody @Valid EmailRequestDTO dto){
        try {
            if(!ValidCheck.isValidEmail(dto.getEmail())) {
                ResponseDTO resDTO = ResponseDTO.builder()
                        .error("이메일 유효성 검사 실패")
                        .build();

                return ResponseEntity.badRequest().body(resDTO);
            }

            if(userService.checkEmail(dto.getEmail())) {
                ResponseDTO resDTO = ResponseDTO.builder()
                        .error("이메일 중복")
                        .build();

                return ResponseEntity.badRequest().body(resDTO);
            }

            mailService.joinEmail(dto.getEmail());

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
    public ResponseEntity<?> registerUser(@RequestBody UserDTO dto) {
        try{
            // 유저 유효성 검사
            if(!ValidCheck.isValidUser(dto)){
                ResponseDTO resDTO = ResponseDTO.builder()
                        .error("유효성 검사 통과 실패")
                        .build();

                return ResponseEntity.badRequest().body(resDTO);
            }

            // 인증번호 확인
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
                    .email(dto.getEmail())
                    .auth(false)
                    .build();

            userService.create(user);
            verificationService.deleteData(dto.getAuthNum()); // 인증 번호 확인 후, redis 상에서 즉시 삭제
            mailService.joinSignupEmail(user.getEmail());

            return ResponseEntity.ok().body(true);
        } catch(Exception e) {
            ResponseDTO resDTO = ResponseDTO.builder().error(e.getMessage()).build();
            return ResponseEntity.badRequest().body(resDTO);
        }
    }

    @PostMapping("/signin")
    public ResponseEntity<?> authenticate(@RequestBody SignInRequestDTO dto) {
        try{
            UserEntity user = userService.getByCredentials(dto.getUserId(), dto.getPassword(), pwdEncoder);

            if(user.getId() != null) {
                final SignInResponseDTO resUserDTO = SignInResponseDTO.builder()
                        .nickname(user.getNickname())
                        .auth(user.isAuth())
                        .accessToken(tokenProvider.createAccessToken(user))
                        .refreshToken(tokenProvider.createRefreshToken(user))
                        .build();

                return ResponseEntity.ok().body(resUserDTO);
            } else {
                // 아이디, 비번으로 찾은 유저 없음 = 로그인 실패
                ResponseDTO resDTO = ResponseDTO.builder()
                        .error(user.getNickname())
                        .build();

                return ResponseEntity.badRequest().body(resDTO);
            }
        } catch(Exception e) {
            ResponseDTO resDTO = ResponseDTO.builder().error(e.getMessage()).build();
            return ResponseEntity.badRequest().body(resDTO);
        }
    }

    @PostMapping("/find/mailSend")
    public ResponseEntity<?> findMailSend(@RequestBody EmailRequestDTO dto) {
        try {
            if(dto.isFlag()) { // 비밀번호 찾기 - check 값이 true
                if (userService.getByUserId(dto.getUserId()) != userService.getByUserEmail(dto.getEmail())) {
                    ResponseDTO resDTO = ResponseDTO.builder()
                            .error("아이디와 이메일의 정보가 일치하지 않습니다.")
                            .build();
                    return ResponseEntity.badRequest().body(resDTO);
                }
            } else { // 아이디 찾기 - check 값이 false
                if(!userService.checkEmail(dto.getEmail())) {
                    ResponseDTO resDTO = ResponseDTO.builder()
                            .error("사용자 정보 없음")
                            .build();

                    return ResponseEntity.badRequest().body(resDTO);
                }
            }

            return ResponseEntity.ok().body(mailService.joinEmail(dto.getEmail()));
        } catch(Exception e) {
            ResponseDTO resDTO = ResponseDTO.builder().error(e.getMessage()).build();
            return ResponseEntity.badRequest().body(resDTO);
        }
    }

    @PostMapping("/findUserId")
    public ResponseEntity<?> findUserId(@RequestBody UserDTO dto) {
        try{
            // 인증번호 확인
            if (!mailService.CheckAuthNum(dto.getEmail(), dto.getAuthNum())) {
                ResponseDTO resDTO = ResponseDTO.builder()
                        .error("이메일 인증 실패")
                        .build();

                return ResponseEntity.badRequest().body(resDTO);
            }

            UserEntity user = userService.getByUserEmail(dto.getEmail());

            String userId = user.getUserId();

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            String date = user.getCreatedAt().format(formatter);

            List<Object> userInfo = new ArrayList<>(Arrays.asList(userId, date));

            ResponseDTO resDTO = ResponseDTO.builder()
                    .data(userInfo)
                    .build();

            verificationService.deleteData(dto.getAuthNum()); // 인증 번호 확인 후, redis 상에서 즉시 삭제
            return ResponseEntity.ok().body(resDTO);
        } catch(Exception e) {
            ResponseDTO resDTO = ResponseDTO.builder().error(e.getMessage()).build();
            return ResponseEntity.badRequest().body(resDTO);
        }
    }

    @PostMapping("/findPassword")
    public ResponseEntity<?> findPassword(@RequestBody UserDTO dto) {
        try{
            // 인증번호 확인
            if (!mailService.CheckAuthNum(dto.getEmail(), dto.getAuthNum())) {
                ResponseDTO resDTO = ResponseDTO.builder()
                        .error("이메일 인증 실패")
                        .build();

                return ResponseEntity.badRequest().body(resDTO);
            }

            UserEntity user = userService.getByUserEmail(dto.getEmail());

            if (userService.getByUserId(dto.getUserId()) != user) {
                ResponseDTO resDTO = ResponseDTO.builder()
                        .error("아이디와 이메일의 정보가 일치하지 않습니다.")
                        .build();
                return ResponseEntity.badRequest().body(resDTO);
            }

            final UserDTO resUserDTO = UserDTO.builder()
                    .id(user.getId())
                    .userId(user.getUserId())
                    .email(user.getEmail())
                    .password(user.getPassword())
                    .nickname(user.getNickname())
                    .accessToken(tokenProvider.createAccessToken(user))
                    .refreshToken(tokenProvider.createRefreshToken(user))
                    .build();

            verificationService.deleteData(dto.getAuthNum()); // 인증 번호 확인 후, redis 상에서 즉시 삭제
            return ResponseEntity.ok().body(resUserDTO);
        } catch(Exception e) {
            ResponseDTO resDTO = ResponseDTO.builder().error(e.getMessage()).build();
            return ResponseEntity.badRequest().body(resDTO);
        }
    }

    @PostMapping("/resetPassword")
    public ResponseEntity<?> restPassword(@AuthenticationPrincipal UserEntity userInfo, @RequestBody UserDTO dto) {
        try{
            if(!ValidCheck.isValidPw(dto.getPassword())){
                ResponseDTO resDTO = ResponseDTO.builder()
                        .error("비밀번호 유효성 검사 실패")
                        .build();
                return ResponseEntity.badRequest().body(resDTO);
            }

            if(!dto.getPassword().equals(dto.getConfirmPassword())){
                ResponseDTO resDTO = ResponseDTO.builder()
                        .error("비밀번호가 일치하지 않음")
                        .build();
                return ResponseEntity.badRequest().body(resDTO);
            }

            UserEntity user = UserEntity.builder()
                    .password(pwdEncoder.encode(dto.getPassword()))
                    .build();

            userService.update(userInfo.getId(), user);

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
            log.info("create new accessToken from : {}", token);

            Claims claims = Jwts.parser()
                    .setSigningKey(jwtProperties.getSecretKey())
                    .parseClaimsJws(token)
                    .getBody();

            UUID id = UUID.fromString(claims.getSubject());

            UserEntity user = userService.getById(id);
            final TokenResponseDTO resUserDTO = TokenResponseDTO.builder()
                    .userId(user.getUserId())
                    .nickname(user.getNickname())
                    .email(user.getEmail())
                    .accessToken(tokenProvider.createAccessToken(user))
                    .build();

            return ResponseEntity.ok().body(resUserDTO);
        }catch (Exception e){
            log.error("/auth/newToken 실행 중 예외 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("newToken fail");
        }
    }
}
