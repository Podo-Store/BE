package PodoeMarket.podoemarket.controller;

import PodoeMarket.podoemarket.Utils.ValidCheck;
import PodoeMarket.podoemarket.config.jwt.JwtProperties;
import PodoeMarket.podoemarket.dto.EmailCheckDTO;
import PodoeMarket.podoemarket.dto.EmailRequestDTO;
import PodoeMarket.podoemarket.dto.ResponseDTO;
import PodoeMarket.podoemarket.dto.UserDTO;
import PodoeMarket.podoemarket.entity.UserEntity;
import PodoeMarket.podoemarket.security.TokenProvider;
import PodoeMarket.podoemarket.service.MailSendService;
import PodoeMarket.podoemarket.service.RedisUtil;
import PodoeMarket.podoemarket.service.UserService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;

import java.time.format.DateTimeFormatter;
import java.util.*;

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

    private final JwtProperties jwtProperties;

    @PostMapping("/checkUserId")
    public ResponseEntity<?> duplicateUserId(@RequestBody UserDTO dto) {
        try {
            if(dto.isCheck()) { // True : 회원가입, False : 비밀번호 찾기
                log.info("check userId");

                if(userService.checkUserId(dto.getUserId())) {
                    ResponseDTO resDTO = ResponseDTO.builder()
                            .error("이미 존재하는 아이디")
                            .build();

                    return ResponseEntity.badRequest().body(resDTO);
                }

                return ResponseEntity.ok().body(true);
            } else {
                log.info("check userId");

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
            log.info("check nickname duplication");

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
    public ResponseEntity<?> mailSend(@RequestBody @Valid EmailRequestDTO emailDTO){
        try {
            if(!ValidCheck.isValidEmail(emailDTO.getEmail())) {
                ResponseDTO resDTO = ResponseDTO.builder()
                        .error("이메일 유효성 검사 실패")
                        .build();

                return ResponseEntity.badRequest().body(resDTO);
            }

            if(emailDTO.isCheck()) { // 회원 가입 시, 이메일 중복 확인
                if(userService.checkEmail(emailDTO.getEmail())) {
                    ResponseDTO resDTO = ResponseDTO.builder()
                            .error("이메일 중복")
                            .build();

                    return ResponseEntity.badRequest().body(resDTO);
                }
            } else { // 아이디 찾기 - check 값이 0
                if(!userService.checkEmail(emailDTO.getEmail())) {
                    ResponseDTO resDTO = ResponseDTO.builder()
                            .error("사용자 정보 없음")
                            .build();

                    return ResponseEntity.badRequest().body(resDTO);
                }
            }

            return ResponseEntity.ok().body(mailService.joinEmail(emailDTO.getEmail()));
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
            log.info("Start signup");

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
            redisUtil.deleteData(dto.getAuthNum()); // 인증 번호 확인 후, redis 상에서 즉시 삭제

            return ResponseEntity.ok().body(true);
        } catch(Exception e) {
            ResponseDTO resDTO = ResponseDTO.builder().error(e.getMessage()).build();
            return ResponseEntity.badRequest().body(resDTO);
        }
    }

    @PostMapping("/signin")
    public ResponseEntity<?> authenticate(@RequestBody UserDTO dto) {
        try{
            log.info("Start signin");

            UserEntity user = userService.getByCredentials(dto.getUserId(), dto.getPassword(), pwdEncoder);
//            log.info("user: {}", user);

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
        } catch(Exception e) {
            ResponseDTO resDTO = ResponseDTO.builder().error(e.getMessage()).build();
            return ResponseEntity.badRequest().body(resDTO);
        }
    }

    @PostMapping("/findUserId")
    public ResponseEntity<?> findUserId(@RequestBody UserDTO dto) {
        try{
            log.info("Start find userId");

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

            redisUtil.deleteData(dto.getAuthNum()); // 인증 번호 확인 후, redis 상에서 즉시 삭제
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

            final String accessToken = tokenProvider.createAccessToken(user);
            final String refreshToken = tokenProvider.createRefreshToken(user);

            final UserDTO resUserDTO = UserDTO.builder()
                    .id(user.getId())
                    .userId(user.getUserId())
                    .email(user.getEmail())
                    .password(user.getPassword())
                    .nickname(user.getNickname())
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .build();

            redisUtil.deleteData(dto.getAuthNum()); // 인증 번호 확인 후, redis 상에서 즉시 삭제
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
            log.info("id : {}", id);

            UserEntity user = userService.getById(id);
            String accessToken = tokenProvider.createAccessToken(user);
            final UserDTO resUserDTO = UserDTO.builder()
                    .userId(user.getUserId())
                    .nickname(user.getNickname())
                    .accessToken(accessToken)
                    .build();

            return ResponseEntity.ok().body(resUserDTO);
        }catch (Exception e){
            log.error("/auth/newToken 실행 중 예외 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("newToken fail");
        }
    }

    // refreshToken 재발급
    @PostMapping("/newRefreshToken")
    public ResponseEntity<?> createNewRefreshToken(HttpServletRequest request){
        try {
            String token = request.getHeader("Authorization").substring(7);
            log.info("create new refresh Token from : {}", token);

            Claims claims = Jwts.parser()
                    .setSigningKey(jwtProperties.getSecretKey())
                    .parseClaimsJws(token)
                    .getBody();

            UUID id = UUID.fromString(claims.getSubject());
            log.info("id : {}", id);

            UserEntity user = userService.getById(id);
            String refreshToken = tokenProvider.createRefreshToken(user);
            final UserDTO resUserDTO = UserDTO.builder()
                    .userId(user.getUserId())
                    .nickname(user.getNickname())
                    .refreshToken(refreshToken)
                    .build();

            return ResponseEntity.ok().body(resUserDTO);
        }catch (Exception e){
            log.error("/auth/newrefreshToken 실행 중 예외 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("newRefreshToken fail");
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated() || authentication.getPrincipal().equals("anonymousUser")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(false);
        }

        return ResponseEntity.ok(true);
    }
}
