package PodoeMarket.podoemarket.user.service;

import PodoeMarket.podoemarket.common.config.jwt.JwtProperties;
import PodoeMarket.podoemarket.common.entity.UserEntity;
import PodoeMarket.podoemarket.common.entity.type.StageType;
import PodoeMarket.podoemarket.common.repository.UserRepository;
import PodoeMarket.podoemarket.common.security.TokenProvider;
import PodoeMarket.podoemarket.service.MailSendService;
import PodoeMarket.podoemarket.service.VerificationService;
import PodoeMarket.podoemarket.user.dto.request.*;
import PodoeMarket.podoemarket.user.dto.response.FindPasswordResponseDTO;
import PodoeMarket.podoemarket.user.dto.response.FindUserIdResponseDTO;
import PodoeMarket.podoemarket.user.dto.response.SignInResponseDTO;
import PodoeMarket.podoemarket.user.dto.response.TokenResponseDTO;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.regex.Pattern;

@RequiredArgsConstructor
@Slf4j
@Service
@Transactional(readOnly = true)
public class UserService {
    private final UserRepository userRepo;
    private final MailSendService mailService;
    private final PasswordEncoder pwdEncoder = new BCryptPasswordEncoder();
    private final VerificationService verificationService;
    private final TokenProvider tokenProvider;
    private final JwtProperties jwtProperties;

    public Boolean validateUserId(UserIdCheckRequestDTO dto) {
        try {
            boolean isExist = userRepo.existsByUserId(dto.getUserId());
            boolean isSignUp = dto.getCheck(); // True : 회원가입, False : 비밀번호 찾기

            return (isSignUp && !isExist) || (!isSignUp && isExist);
        } catch (RuntimeException e) {
            throw new RuntimeException("아이디 확인 실패", e);
        }
    }

    public void checkNickname(final String nickname) {
        try {
            boolean isExist = userRepo.existsByNickname(nickname);

            if (isExist)
                throw new RuntimeException("닉네임 중복");
        } catch (Exception e) {
            throw e;
        }
    }

    public void validateAndSendEmail(final String email) {
        try {
            if(isValidEmail(email) && userRepo.existsByEmail(email))
                throw new RuntimeException("이메일 중복");

            mailService.joinEmail(email);
        } catch (Exception e) {
            throw e;
        }
    }

    @Transactional
    public void create(final SignUpRequestDTO dto) {
        try {
            if(!isValidUserId(dto.getUserId()))
                throw new RuntimeException("아이디 유효성 검사 실패");

            if(!isValidEmail(dto.getEmail()))
                throw new RuntimeException("이메일 유효성 검사 실패");

            if(!isValidPassword(dto.getPassword(), dto.getConfirmPassword()))
                throw new RuntimeException("비밀번호 유효성 검사 실패");

            if(!isValidNickname(dto.getNickname()))
                throw new RuntimeException("닉네임 유효성 검사 실패");

            if(userRepo.existsByUserId(dto.getUserId()))
                throw new RuntimeException("이미 존재하는 아이디");

            if(userRepo.existsByEmail(dto.getEmail()))
                throw new RuntimeException("이미 존재하는 이메일");

            if(userRepo.existsByNickname(dto.getNickname()))
                throw new RuntimeException("이미 존재하는 닉네임");

            UserEntity user = UserEntity.builder()
                    .userId(dto.getUserId())
                    .password(pwdEncoder.encode(dto.getPassword()))
                    .nickname(dto.getNickname())
                    .email(dto.getEmail())
                    .auth(false) // 명시적 선언
                    .stageType(StageType.DEFAULT) // 명시적 선언
                    .build();

            userRepo.save(user);
            verificationService.deleteData(dto.getAuthNum()); // 인증 번호 확인 후, redis 상에서 즉시 삭제
            mailService.joinSignupEmail(dto.getEmail());
        } catch (Exception e){
            throw e;
        }
    }

    public SignInResponseDTO getByCredentials(final String userId, final String password){
        try {
            final UserEntity user = userRepo.findByUserId(userId);

            if(user != null && pwdEncoder.matches(password, user.getPassword())) {
                return SignInResponseDTO.builder()
                        .nickname(user.getNickname())
                        .auth(user.isAuth())
                        .accessToken(tokenProvider.createAccessToken(user))
                        .refreshToken(tokenProvider.createRefreshToken(user))
                        .build();
            }
            else
                throw new RuntimeException("아이디 혹은 비밀번호가 일치하지 않음");

        } catch (Exception e){
            throw e;
        }
    }

    public String findUserInfo(final EmailRequestDTO dto) {
        try {
            if(dto.getFlag()) { // 비밀번호 찾기 - check 값이 true
                final UserEntity userById = userRepo.findByUserId(dto.getUserId());
                final UserEntity userByEmail = userRepo.findByEmail(dto.getEmail());

                if(userById == null || userByEmail == null || !userById.getUserId().equals(dto.getUserId()))
                    throw new RuntimeException("아이디와 이메일의 정보가 일치하지 않습니다.");
            } else { // 아이디 찾기 - check 값이 false
                if(!userRepo.existsByEmail(dto.getEmail()))
                    throw new RuntimeException("사용자 정보 없음");
            }

            return mailService.joinEmail(dto.getEmail());
        } catch (RuntimeException e) {
            throw e;
        }
    }

    public FindUserIdResponseDTO findUserId(final FindUserIdRequestDTO dto) {
        try {
            final UserEntity user = userRepo.findByEmail(dto.getEmail());

            if(user == null)
                throw new RuntimeException("사용자 정보를 찾을 수 없습니다.");

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

            verificationService.deleteData(dto.getAuthNum()); // 인증 번호 확인 후, redis 상에서 즉시 삭제

            return FindUserIdResponseDTO.builder()
                    .userId(user.getUserId())
                    .date(user.getCreatedAt().format(formatter))
                    .build();
        } catch (RuntimeException e) {
            throw e;
        }
    }

    public FindPasswordResponseDTO findPassword(final FindPasswordRequestDTO dto) {
        try {
            final UserEntity userByEmail = userRepo.findByEmail(dto.getEmail());
            final UserEntity userById = userRepo.findByUserId(dto.getUserId());

            if(userByEmail == null || userById == null || !userByEmail.getId().equals(userById.getId()))
                throw new RuntimeException("아이디와 이메일의 정보가 일치하지 않습니다.");

            verificationService.deleteData(dto.getAuthNum()); // 인증 번호 확인 후, redis 상에서 즉시 삭제

            return FindPasswordResponseDTO.builder()
                    .id(userById.getId())
                    .userId(userById.getUserId())
                    .email(userById.getEmail())
                    .password(userById.getPassword())
                    .nickname(userById.getNickname())
                    .accessToken(tokenProvider.createAccessToken(userById))
                    .refreshToken(tokenProvider.createRefreshToken(userById))
                    .build();
        } catch (RuntimeException e) {
            throw e;
        }
    }

    @Transactional
    public void updatePassword(final UUID id, final PwCheckRequestDTO dto) {
        try {
            if(!isValidPassword(dto.getPassword(), dto.getConfirmPassword()))
                throw new RuntimeException("비밀번호 유효성 검사 실패");

            UserEntity user = userRepo.findById(id);

            if(user == null)
                throw new RuntimeException("사용자 정보를 찾을 수 없습니다.");

            user.setPassword(pwdEncoder.encode(dto.getPassword()));

            userRepo.save(user);
        } catch (RuntimeException e) {
            throw e;
        }
    }

    public TokenResponseDTO createNewToken(final String token) {
        try {
            Claims claims = Jwts.parser()
                    .setSigningKey(jwtProperties.getSecretKey())
                    .parseClaimsJws(token)
                    .getBody();

            UUID userId = UUID.fromString(claims.getSubject());

            final UserEntity user = userRepo.findById(userId);

            if(user == null)
                throw new RuntimeException("사용자 정보를 찾을 수 없습니다.");

            return TokenResponseDTO.builder()
                    .userId(user.getUserId())
                    .nickname(user.getNickname())
                    .email(user.getEmail())
                    .accessToken(tokenProvider.createAccessToken(user))
                    .build();
        } catch (RuntimeException e) {
            throw e;
        }
    }

    // ============ private method ================

    private Boolean isValidEmail(final String email) {
        try {
            String regx_email = "^[\\w-]+(\\.[\\w-]+)*@[\\w-]+(\\.[\\w-]+)*(\\.[a-zA-Z]{2,})$";

            return email != null && !email.isBlank() && Pattern.matches(regx_email, email);
        } catch (Exception e) {
            throw new RuntimeException("이메일 유효성 검사 실패", e);
        }
    }

    private Boolean isValidPassword(final String password, final String confirmPassword) {
        try {
            String regx_pwd = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[$@!%*#?&])[A-Za-z\\d$@!%*#?&]{5,11}$"; // 숫자 최소 1개, 대소문자 최소 1개, 특수문자 최소 1개, (5-11)

            if(password == null || password.isBlank())
                return false;
            else if(password.length() < 4 || password.length() > 12)
                return false;
            else if(!password.equals(confirmPassword))
                return false;
            else if(!Pattern.matches(regx_pwd, password))
                return false;

            return true;
        } catch (Exception e) {
            throw new RuntimeException("비밀번호 유효성 검사 실패", e);
        }
    }

    private Boolean isValidUserId(final String userId) {
        try {
            String regx_userId = "^(?=.*[a-zA-Z])(?=.*[0-9])[a-zA-Z0-9]{5,10}$"; // 영어, 숫자, (5-10)

            return userId != null && !userId.isBlank() && Pattern.matches(regx_userId, userId);
        } catch (Exception e) {
            throw new RuntimeException("아이디 유효성 검사 실패", e);
        }
    }

    private Boolean isValidNickname(final String nickname) {
        try {
            String regx_nick = "^[가-힣a-zA-Z0-9]{3,8}$"; // 한글, 영어, 숫자, (-8)

            return nickname != null && !nickname.isBlank() && Pattern.matches(regx_nick, nickname);
        } catch (Exception e) {
            throw new RuntimeException("닉네임 유효성 검사 실패", e);
        }
    }
}
