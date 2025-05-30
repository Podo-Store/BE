package PodoeMarket.podoemarket.common.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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

    @Builder.Default
    @Column(nullable = false)
    @ColumnDefault("false")
    private Boolean script = false; // 대본권 구매 여부

    @Builder.Default
    @Column(nullable = false)
    @ColumnDefault("0")
    private Integer scriptPrice = 0;

    @Builder.Default
    @Column(nullable = false)
    @ColumnDefault("0")
    private Integer performanceAmount = 0;

    @Builder.Default
    @Column
    @ColumnDefault("0")
    private Integer performancePrice = 0;

    @Column(nullable = false)
    private Integer totalPrice;

    @Column(nullable = false)
    private String title;

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
    @JsonIgnore
    private OrdersEntity order;

    // product : orderItem = 1 : N
    @ManyToOne(targetEntity = ProductEntity.class)
    @JoinColumn(name = "product_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JsonIgnore
    private ProductEntity product;

    // user : orderItem = 1 : N
    @ManyToOne(targetEntity = UserEntity.class)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private UserEntity user;

    // orderItem : performanceDate = 1 : N
    @OneToMany(mappedBy = "orderItem", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<PerformanceDateEntity> performanceDate = new ArrayList<>();

    // orderItem : applicant = 1 : 1
    @OneToOne(mappedBy = "orderItem")
    @JsonIgnore
    private ApplicantEntity applicant;
}
