package PodoeMarket.podoemarket.service;

import PodoeMarket.podoemarket.dto.UserDTO;
import PodoeMarket.podoemarket.entity.UserEntity;
import PodoeMarket.podoemarket.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@RequiredArgsConstructor
@Service
@Slf4j
public class UserService {
    private final UserRepository repo;
    public UserEntity create(final UserEntity userEntity) {
        final String email = userEntity.getEmail();
        final String password = userEntity.getPassword();
        final String phonenumber = userEntity.getPhoneNumber();
        final String nickname = userEntity.getNickname();

//        // user 정보 확인 - 필드 하나라도 비어있을 경우 확인
//        if(userEntity == null) {
//            throw new RuntimeException("some fields are empty");
//        }
//
//        // 아이디(이메일) 확인
//        if(email = )

        return repo.save(userEntity);
    }
}
