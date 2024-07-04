package PodoeMarket.podoemarket.repository;

import PodoeMarket.podoemarket.entity.QnAEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface QnARepository extends JpaRepository<QnAEntity, Long> {
    List<QnAEntity> findAllByUserId(UUID id);
}
