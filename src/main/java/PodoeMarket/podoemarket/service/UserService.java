package PodoeMarket.podoemarket.service;

import PodoeMarket.podoemarket.entity.UserEntity;
import PodoeMarket.podoemarket.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Slf4j
@Service
public class UserService {
    private final UserRepository repo;

    public UserEntity create(final UserEntity userEntity) {
        final String email = userEntity.getEmail();
        final String password = userEntity.getPassword();
        final String nickname = userEntity.getNickname();

        // user 정보 확인 - 필드 하나라도 비어있을 경우 확인
        if (userEntity == null) {
            throw new RuntimeException("Invalid arguments");
        }

        // 이메일
        if (email == null || email.isBlank()) {
            throw new RuntimeException("UserId is invalid arguments");
        }

        if (repo.existsByEmail(email)) {
            log.warn("email already exists {}", email);
            throw new RuntimeException("UserId already exists");
        }

        // 비밀번호
        if (password == null) {
            log.info(password);
            throw new RuntimeException("Password is invalid arguments");
        }

        // 닉네임
        if (nickname == null || nickname.isBlank()) {
            throw new RuntimeException("Nickname is invalid arguments");
        }

        return repo.save(userEntity);
    }

    // [before] 패스워드 암호화 적용 전
//  public UserEntity getByCredentials(final String email, String password) {
//    return repository.findByEmailAndPassword(email, password);
//  }

    // [after] 패스워드 암호화 적용 후
    // - BCryptPasswordEncoder 를 이용해 비밀번호 암호화 구현 (같은 비밀번호더라도 인코딩한 결과 값이 달라짐)
    // - 왜? 인코딩할 때마다 의미없는 값을 붙여서 암호화한 결과를 생성하기 때문
    //    - Salt: 의미없는 값
    //    - Salting: Salt 를 붙여서 인코딩하는 것

    // ex
    // 유저 test1; 비번 1234 + as3r2r => ael;a fa;ekfjasfka;lfjaweoifasdfe
    // 유저 test2; 비번 1234 + eaexfe => qe;izkv xckvndfkjvna;eksfkwe;kwef
//    public UserEntity getByCredentials(final String email, final String password, final PasswordEncoder encoder) {
////        final UserEntity originalUser = repository.findByEmail(email);
//
//        // matches() 메소드 이용해서 패스워드 동일 여부 비교
//        if (originalUser != null && encoder.matches(password, originalUser.getPassword())) {
//            return originalUser;
//        }
//
//        return null;
//    }
}
