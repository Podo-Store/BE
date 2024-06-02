package PodoeMarket.podoemarket.repository;

import PodoeMarket.podoemarket.entity.WishScriptEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WishScriptRepository extends JpaRepository<WishScriptEntity, Long> {
    WishScriptEntity findById(UUID id);
}
