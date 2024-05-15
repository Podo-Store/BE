package PodoeMarket.podoemarket.repository;

import PodoeMarket.podoemarket.entity.WishScriptEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WishScriptRepository extends JpaRepository<WishScriptEntity, Long> {
}
