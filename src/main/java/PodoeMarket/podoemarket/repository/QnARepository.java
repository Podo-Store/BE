package PodoeMarket.podoemarket.repository;

import PodoeMarket.podoemarket.entity.QnAEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface QnARepository extends JpaRepository<QnAEntity, Long> {
    List<QnAEntity> findAllByStatus(boolean status);

    List<QnAEntity> findAllByUserIdAndStatus(UUID id, boolean status);

    List<QnAEntity> findAllByStatusAndQuestionContaining(boolean status, String keyword);

    List<QnAEntity> findAllByUserIdAndStatusAndQuestionContaining(UUID id, boolean status, String keyword);
}
