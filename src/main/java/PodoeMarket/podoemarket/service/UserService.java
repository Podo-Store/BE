package PodoeMarket.podoemarket.service;

import PodoeMarket.podoemarket.entity.UserEntity;
import PodoeMarket.podoemarket.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@RequiredArgsConstructor
@Slf4j
@Service
public class UserService {
    private final UserRepository userRepo;

    public void create(final UserEntity userEntity) {
        final String userId = userEntity.getUserId();
        final String email = userEntity.getEmail();
        final String password = userEntity.getPassword();
        final String nickname = userEntity.getNickname();

        // user 정보 확인 - 필드 하나라도 비어있을 경우 확인
        if(userEntity == null) {
            throw new RuntimeException("Invalid arguments");
        }

        // 아이디
        if(userId == null || userId.isBlank()) {
            throw new RuntimeException("UserId is invalid arguments");
        }

        if(userRepo.existsByUserId(userId)) {
            log.warn("userId already exists {}", userId);
            throw new RuntimeException("UserId already exists");
        }

        // 이메일
        if(email == null || email.isBlank()) {
            throw new RuntimeException("Email is invalid arguments");
        }

        if(userRepo.existsByEmail(email)) {
            log.warn("email already exists {}", email);
            throw new RuntimeException("Email already exists");
        }

        // 비밀번호
        if(password == null) {
            log.info(password);
            throw new RuntimeException("Password is invalid arguments");
        }

        // 닉네임
        if(nickname == null || nickname.isBlank()) {
            throw new RuntimeException("Nickname is invalid arguments");
        }

        if(userRepo.existsByNickname(nickname)) {
            log.warn("nickname already exists {}", nickname);
            throw new RuntimeException("Nickname already exists");
        }

        userRepo.save(userEntity);
    }

    public UserEntity getByCredentials(final String userId, final String password, final PasswordEncoder encoder){
        log.info("find user by userId");

        try {
            final UserEntity originalUser = userRepo.findByUserId(userId);
//            log.info("original User: {}", originalUser);

            if(originalUser != null && encoder.matches(password, originalUser.getPassword())) {
                return originalUser;
            } else if (originalUser == null) {
                // 로그인 실패 시, 실패 이유 전달을 위한 메세지 작성
                UserEntity user = new UserEntity();
                user.setNickname("잘못된 아이디");

                return user;
            } else if(!encoder.matches(password, originalUser.getPassword())) {
                UserEntity user = new UserEntity();
                user.setNickname("잘못된 비밀번호");

                return user;
            } else{
                log.info("signin error");
                return null;
            }
        } catch (Exception e){
            log.error("UserService.getByCredentials 메소드 중 예외 발생", e);
            return null;
        }
    }

    public Boolean checkUserId(final String userId) {
        return userRepo.existsByUserId(userId);
    }

    public Boolean checkEmail(final String email) {
        return userRepo.existsByEmail(email);
    }

    public Boolean checkNickname(final String nickname) {
        return userRepo.existsByNickname(nickname);
    }

    public UserEntity getByUserEmail(final String email) {
        return userRepo.findByEmail(email);
    }

    public UserEntity getById(final UUID id) {
        return userRepo.findById(id);
    }

    public UserEntity update(UUID id, final UserEntity userEntity) {
        final UserEntity user = userRepo.findById(id);

        user.setPassword(userEntity.getPassword());

        return userRepo.save(user);
    }

    public UserEntity getByUserId(final String userId) {
        return userRepo.findByUserId(userId);
    }
}
