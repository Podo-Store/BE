package PodoeMarket.podoemarket.service;

import PodoeMarket.podoemarket.entity.ProductInfoEntity;
import PodoeMarket.podoemarket.entity.UserEntity;
import PodoeMarket.podoemarket.repository.ProductInfoRepository;
import PodoeMarket.podoemarket.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.UUID;

@RequiredArgsConstructor
@Slf4j
@Service
public class MypageService {
    private final UserRepository userRepo;
    private final ProductInfoRepository productInfoRepo;

    public UserEntity update(UUID id, final UserEntity userEntity) {
        final String password = userEntity.getPassword();
        final String nickname = userEntity.getNickname();
        final String type = userEntity.getType();
        final String filepath = userEntity.getFilePath();

        final UserEntity user = userRepo.findById(id);

        if(!user.getNickname().equals(nickname)){
            if(userRepo.existsByNickname(nickname)){
                throw new RuntimeException("Nickname is already exists");
            }
        }


        if(userEntity.getType() != null && userEntity.getFilePath() != null) {
            if(!Objects.equals(type, "image/jpeg") && !Objects.equals(type, "image/png")) {
                throw new RuntimeException("file type is wrong");
            }
        }

        user.setPassword(password);
        user.setNickname(nickname);
        user.setType(type);
        user.setFilePath(filepath);

        return userRepo.save(user);
    }

    public Boolean checkUser(UUID id, final String password, final PasswordEncoder encoder) {
        try{
            final UserEntity originalUser = userRepo.findById(id);

            if(originalUser != null && encoder.matches(password, originalUser.getPassword()))
                return true;
            else
                return false;
        } catch (Exception e){
            log.error("MypageService.checkUser 메소드 중 예외 발생", e);
            return false;
        }
    }

    public UserEntity originalUser(UUID id) {
        final UserEntity originalUser = userRepo.findById(id);

        return originalUser;
    }

    public ProductInfoEntity product(UUID id) {
        final UserEntity product = userRepo.findById(id);
        
        // 사용자의 id로 조회한 사용자가 product info만 전달하도록 조회 필요
    }
}
