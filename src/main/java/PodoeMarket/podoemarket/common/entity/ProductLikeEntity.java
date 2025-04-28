package PodoeMarket.podoemarket.common.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.minidev.json.annotate.JsonIgnore;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

@Entity
@Table(name = "productLike")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProductLikeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist // entity가 영속화되기 직전에 실행
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
        createdAt = now;
    }

    // user : like = 1 : N
    @ManyToOne(targetEntity = UserEntity.class, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @JsonIgnore
    private UserEntity user;

    // product : like = 1 : N
    @ManyToOne(targetEntity = ProductEntity.class, fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    @JsonIgnore
    private ProductEntity product;
}
