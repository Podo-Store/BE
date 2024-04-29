package PodoeMarket.podoemarket.repository;

import PodoeMarket.podoemarket.entity.ScriptEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<ScriptEntity, Long> {
    Optional<ScriptEntity> findByName(String fileName);
}
