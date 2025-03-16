package PodoeMarket.podoemarket.common.repository;

import PodoeMarket.podoemarket.common.entity.OrdersEntity;
import PodoeMarket.podoemarket.common.entity.type.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<OrdersEntity, Long> {
    Long countAllByOrderStatus(OrderStatus orderStatus);
    Page<OrdersEntity> findAllByOrderStatus(OrderStatus orderStatus, Pageable pageable);
}
