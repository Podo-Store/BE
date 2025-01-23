package PodoeMarket.podoemarket.service;

import PodoeMarket.podoemarket.common.entity.UserEntity;
import PodoeMarket.podoemarket.common.repository.UserRepository;
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
            throw new RuntimeException("항목들이 올바르지 않음");
        }

        // 아이디
        if(userId == null || userId.isBlank()) {
            throw new RuntimeException("userId가 올바르지 않음");
        }

        if(userRepo.existsByUserId(userId)) {
            throw new RuntimeException("이미 존재하는 UserId");
        }

        // 이메일
        if(email == null || email.isBlank()) {
            throw new RuntimeException("email이 올바르지 않음");
        }

        if(userRepo.existsByEmail(email)) {
            throw new RuntimeException("이미 존재하는 email");
        }

        // 비밀번호
        if(password == null) {
            throw new RuntimeException("password가 올바르지 않음");
        }

        // 닉네임
        if(nickname == null || nickname.isBlank()) {
            throw new RuntimeException("nickname이 올바르지 않음");
        }

        if(userRepo.existsByNickname(nickname)) {
            throw new RuntimeException("이미 존재하는 nickname");
        }

        userRepo.save(userEntity);
    }

    public UserEntity getByCredentials(final String userId, final String password, final PasswordEncoder encoder){
        try {
            final UserEntity originalUser = userRepo.findByUserId(userId);

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

    public void update(UUID id, final UserEntity userEntity) {
        final UserEntity user = userRepo.findById(id);

        user.setPassword(userEntity.getPassword());

        userRepo.save(user);
    }

    public UserEntity getByUserId(final String userId) {
        return userRepo.findByUserId(userId);
    }
}
