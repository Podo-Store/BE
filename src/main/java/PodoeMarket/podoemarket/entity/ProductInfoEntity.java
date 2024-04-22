package PodoeMarket.podoemarket.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "product_info")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProductInfoEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false)
    private int price;

    @Column(nullable = false)
    private boolean performance;

    @Column
    private int performancePrice;

    @Column(nullable = false)
    private String content;

    @Column(nullable = false)
    private int status;
    // 0 : 판매 중, 1 : 판매 중단

    @Column(nullable = false)
    private LocalDate date;

    @PrePersist
    protected void onCreate() {
        date = LocalDate.now(ZoneId.of("Asia/Seoul"));
    }
    @PreUpdate
    protected void onUpdate() {date= LocalDate.now(ZoneId.of("Asia/Seoul"));}

    // user : product_info = 1 : N
    @ManyToOne(targetEntity = UserEntity.class)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    // product_info : basket = 1 : N
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BasketEntity> basket = new ArrayList<>();

    // product_info : favorite = 1 : N
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductLikeEntity> product_like = new ArrayList<>();

    // product : product_info = 1 : 1
    @OneToOne
    private ProductEntity product;
}
