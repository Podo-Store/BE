package PodoeMarket.podoemarket.common.repository;

import PodoeMarket.podoemarket.common.entity.OrdersEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<OrdersEntity, Long> {
    Long countAllByPaymentStatus(Boolean paymentStatus);
    Page<OrdersEntity> findAllByPaymentStatus(Boolean paymentStatus, Pageable pageable);
}
