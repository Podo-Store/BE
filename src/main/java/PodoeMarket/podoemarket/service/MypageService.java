package PodoeMarket.podoemarket.service;

import PodoeMarket.podoemarket.entity.UserEntity;
import PodoeMarket.podoemarket.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@RequiredArgsConstructor
@Slf4j
@Service
public class MypageService {
    private final UserRepository userRepo;
    private final PasswordEncoder pwdEncoder = new BCryptPasswordEncoder();

    public UserEntity update(UUID id, final UserEntity userEntity) {
        final String password = userEntity.getPassword();
        final String nickname = userEntity.getNickname();
        final String phoneNumber = userEntity.getPhoneNumber();
        final String email = userEntity.getEmail();

        final UserEntity user = userRepo.findById(id);

        log.info("update user: {}", user);

        if(userEntity == null) {
            throw new RuntimeException("Invalid arguments");
        }

        if(userRepo.existsByNickname(nickname)) {
            throw new RuntimeException("Nickname is already exist");
        }

        if(nickname == null || nickname.trim().isEmpty()) {
            throw new RuntimeException("Nickname is invalid arguments");
        }

        if(email == null || email.trim().isEmpty()) {
            throw new RuntimeException("Email is invalid arguments");
        }

        if(userRepo.existsByEmail(email)) {
            throw new RuntimeException("Email is already exist");
        }

        user.setPassword(pwdEncoder.encode(password));
        user.setNickname(nickname);
        user.setPhoneNumber(phoneNumber);
        user.setEmail(email);

        return userRepo.save(user);
    }
}
