package PodoeMarket.podoemarket.common.repository;

import PodoeMarket.podoemarket.common.entity.UserAccessLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAccessLogRepository extends JpaRepository<UserAccessLogEntity, Long> {
}
