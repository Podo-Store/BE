package PodoeMarket.podoemarket.repository;

import PodoeMarket.podoemarket.entity.FileEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FileRepository extends JpaRepository<FileEntity, Long> {
    // 추가적인 메서드가 필요한 경우 여기에 선언
}
