package PodoeMarket.podoemarket.repository;

import PodoeMarket.podoemarket.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {
    Boolean existsByUserId(String userId);
    UserEntity findByUserId(String userId);
    UserEntity findById(UUID id);
    Boolean existsByEmail(String email);
    Boolean existsByNickname(String nickname);
    UserEntity findByEmail(String email);
}
