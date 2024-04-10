package PodoeMarket.podoemarket.repository;

import PodoeMarket.podoemarket.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {
    Boolean existsByUserId(String userId);
    Boolean existsByEmail(String email);
    UserEntity findByUserId(String userId);
    Boolean existsByNickname(String nickname);
}
