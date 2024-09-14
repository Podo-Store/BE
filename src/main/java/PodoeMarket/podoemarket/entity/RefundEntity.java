package PodoeMarket.podoemarket.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "refund")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RefundEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false)
    private int price;

    @Column(nullable = false, length = 51)
    private String content;

    // order : refund = 1 : N
    @ManyToOne(targetEntity = OrdersEntity.class)
    @JoinColumn(name = "order_id", nullable = false)
    private OrdersEntity order;

    // user : refund = 1 : N
    @ManyToOne(targetEntity = UserEntity.class)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;
}