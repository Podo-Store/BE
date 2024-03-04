package PodoeMarket.podoemarket.service;

import PodoeMarket.podoemarket.entity.UserEntity;
import PodoeMarket.podoemarket.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
// 스프링 시큐리티에서 사용자 정보를 가져오는 인터페이스
public class UserDetailService implements UserDetailsService {
    private final UserRepository userRepository;

    // 사용자 이름(userId)로 사용자 정보를 가져오는 메서드
    @Override
    public UserEntity loadUserByUsername(String email) {
        return userRepository.findByEmail(email).orElseThrow(() -> new IllegalArgumentException(email));
    }
}
