package PodoeMarket.podoemarket.common.entity;

import PodoeMarket.podoemarket.common.entity.type.PlayType;
import PodoeMarket.podoemarket.common.entity.type.ProductStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "product")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProductEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String writer;

    @Column(nullable = false)
    private String filePath;

    @Column
    private String imagePath;

    @Column(nullable = false)
    @ColumnDefault("false")
    private Boolean script; // 대본권 판매 여부

    @Column(nullable = false)
    @ColumnDefault("0")
    private Integer scriptPrice;

    @Column(nullable = false)
    @ColumnDefault("false")
    private Boolean performance; // 공연권 판매 여부

    @Column
    @ColumnDefault("0")
    private Integer performancePrice;

    @Column
    private String descriptionPath;

    @Enumerated(EnumType.STRING)
    @Column
    private PlayType playType;

    @Column
    private String plot;

    @Column(nullable = false)
    @ColumnDefault("0")
    private Long viewCount;

    // 관리자(심사 주체) 확인 여부
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductStatus checked = ProductStatus.WAIT;

    @Column(nullable = false)
    @ColumnDefault("0")
    private Integer any;

    @Column(nullable = false)
    @ColumnDefault("0")
    private Integer male;

    @Column(nullable = false)
    @ColumnDefault("0")
    private Integer female;

    @Column
    private String stageComment;

    @Column(nullable = false)
    @ColumnDefault("0")
    private Integer runningTime;

    @Column(nullable = false)
    @ColumnDefault("0")
    private Integer scene; // 장

    @Column(nullable = false)
    @ColumnDefault("0")
    private Integer act; // 막

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

    // user : product = 1 : N
    @ManyToOne(targetEntity = UserEntity.class, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @JsonIgnore
    private UserEntity user;

    // product : orderItem = 1 : N
    @OneToMany(mappedBy = "product", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<OrderItemEntity> orderItem = new ArrayList<>();

    // product : like = 1 : N
    @OneToMany(mappedBy = "product", fetch = FetchType.LAZY)
    private List<ProductLikeEntity> like = new ArrayList<>();
}
