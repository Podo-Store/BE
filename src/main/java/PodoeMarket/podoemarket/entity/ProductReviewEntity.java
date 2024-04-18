package PodoeMarket.podoemarket.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

@Entity
@Table(name="product_review")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProductReviewEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    // 만족도 (5점 만점)
    @Column(nullable = false)
    @ColumnDefault("3")
    private int satisfaction;

    // 키워드(1 : 최고에요, 2 : 추천해요, 3 : 재밌어요)
    @Column(nullable = false)
    private int keyword;

    @Column(nullable = false, length = 20)
    private String content;

    @Column(nullable = false)
    private LocalDate date;

    @PrePersist // entity가 영속화되기 직전에 실행
    protected void onCreate() {
        date = LocalDate.now(ZoneId.of("Asia/Seoul"));
    }
    @PreUpdate // db에 entity가 업데이트되기 직전에 실행
    protected void onUpdate() {date= LocalDate.now(ZoneId.of("Asia/Seoul"));}

    // user : product_review = 1 : N
    @ManyToOne(targetEntity = UserEntity.class)
    @JoinColumn(name="user_id", nullable = false)
    private UserEntity user;

    // product : product_review = 1 : N
    @ManyToOne(targetEntity = ProductEntity.class)
    @JoinColumn(name="product_id", nullable = false)
    private ProductEntity product;
}
