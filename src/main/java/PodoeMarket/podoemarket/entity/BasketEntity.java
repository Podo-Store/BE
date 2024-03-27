package PodoeMarket.podoemarket.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "basket")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BasketEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    // 회원 시퀀스 아이디 uuid
    // 작품 시퀀스 아이디 uuid

    // user : basket = 1 : N
    @ManyToOne(targetEntity = UserEntity.class)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    // product : basket = 1 : N
    @ManyToOne(targetEntity = ProductInfoEntity.class)
    @JoinColumn(name = "product_id", nullable = false)
    private ProductInfoEntity product;
}
