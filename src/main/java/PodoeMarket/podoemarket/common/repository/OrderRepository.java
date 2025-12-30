package PodoeMarket.podoemarket.common.repository;

import PodoeMarket.podoemarket.common.entity.OrdersEntity;
import PodoeMarket.podoemarket.common.entity.type.OrderStatus;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;

public interface OrderRepository extends JpaRepository<OrdersEntity, Long> {
    OrdersEntity findById(long id);

    @Modifying
    @Query("""
    DELETE FROM OrdersEntity o
    WHERE o.orderStatus = :status
    AND o.createdAt < :expiredAt
""")
    int deleteExpiredOrders(@Param("status") OrderStatus status, @Param("expiredAt") LocalDateTime expiredAt);
}
