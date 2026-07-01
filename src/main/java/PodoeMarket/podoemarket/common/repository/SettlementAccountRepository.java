package PodoeMarket.podoemarket.common.repository;

import PodoeMarket.podoemarket.common.entity.SettlementAccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SettlementAccountRepository extends JpaRepository<SettlementAccountEntity, UUID> {
    SettlementAccountEntity findByUserId(UUID userId);
}
