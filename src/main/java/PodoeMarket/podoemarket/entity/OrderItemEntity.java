package PodoeMarket.podoemarket.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;
import java.util.stream.Stream;

@Entity
@Table(name = "orderItem")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderItemEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false)
    @ColumnDefault("False")
    private boolean script; // 대본권 구매 여부

    @Column(nullable = false)
    @ColumnDefault("0")
    private int scriptPrice;

    @Column(nullable = false)
    @ColumnDefault("False")
    private boolean performance; // 공연권 구매 여부

    @Column
    @ColumnDefault("0")
    private int performancePrice;

    @Column(nullable = false)
    @ColumnDefault("0")
    private int contractStatus; // 0: 공연권 판매 안함, 1: 공연권 구매, 2: 계약 중, 3: 계약 완료

    @Column(nullable = false)
    private int totalPrice;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist // entity가 영속화되기 직전에 실행
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
        createdAt = now;
        updatedAt = now;
    }
    @PreUpdate // db에 entity가 업데이트되기 직전에 실행
    protected void onUpdate() { updatedAt = LocalDateTime.now(ZoneId.of("Asia/Seoul")); }


    // orders : orderItem = 1 : N
    @ManyToOne(targetEntity = OrdersEntity.class)
    @JoinColumn(name = "order_id", nullable = false)
    private OrdersEntity order;

    // product : orderItem = 1 : N
    @ManyToOne(targetEntity = ProductEntity.class)
    @JoinColumn(name = "product_id", nullable = false)
    private ProductEntity product;
}
