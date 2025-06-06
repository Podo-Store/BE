package PodoeMarket.podoemarket.common.repository;

import PodoeMarket.podoemarket.common.entity.OrdersEntity;
import PodoeMarket.podoemarket.common.entity.type.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<OrdersEntity, Long> {
    Long countAllByOrderStatus(OrderStatus orderStatus);

    Page<OrdersEntity> findAllByOrderStatus(OrderStatus orderStatus, Pageable pageable);

    @Query("SELECT o FROM OrdersEntity o " +
            "JOIN FETCH o.user " +
            "JOIN FETCH o.orderItem oi " +
            "JOIN FETCH oi.product " +
            "WHERE o.id = :orderId")
    OrdersEntity findOrderById(@Param("orderId") Long orderId);
}
