package PodoeMarket.podoemarket.repository;

import PodoeMarket.podoemarket.entity.ScriptEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FileRepository extends JpaRepository<ScriptEntity, Long> {
    // 추가적인 메서드가 필요한 경우 여기에 선언
}
