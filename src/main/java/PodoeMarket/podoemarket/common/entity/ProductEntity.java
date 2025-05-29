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

    @Builder.Default
    @Column(nullable = false)
    @ColumnDefault("false")
    private Boolean script = false; // 대본권 판매 여부

    @Builder.Default
    @Column(nullable = false)
    @ColumnDefault("0")
    private Integer scriptPrice = 0;

    @Builder.Default
    @Column(nullable = false)
    @ColumnDefault("false")
    private Boolean performance = false; // 공연권 판매 여부

    @Column
    @ColumnDefault("0")
    private Integer performancePrice = 0;

    @Column
    private String descriptionPath;

    @Enumerated(EnumType.STRING)
    @Column
    private PlayType playType;

    @Column
    private String plot;

    @Builder.Default
    @Column(nullable = false)
    @ColumnDefault("0")
    private Long viewCount = 0L;

    @Builder.Default
    @Column(nullable = false)
    @ColumnDefault("0")
    private Long likeCount = 0L;

    // 관리자(심사 주체) 확인 여부
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductStatus checked = ProductStatus.WAIT;

    @Builder.Default
    @Column(nullable = false)
    @ColumnDefault("0")
    private Integer any = 0;

    @Builder.Default
    @Column(nullable = false)
    @ColumnDefault("0")
    private Integer male = 0;

    @Builder.Default
    @Column(nullable = false)
    @ColumnDefault("0")
    private Integer female = 0;

    @Column
    private String stageComment;

    @Builder.Default
    @Column(nullable = false)
    @ColumnDefault("0")
    private Integer runningTime = 0;

    @Builder.Default
    @Column(nullable = false)
    @ColumnDefault("0")
    private Integer scene = 0; // 장

    @Builder.Default
    @Column(nullable = false)
    @ColumnDefault("0")
    private Integer act = 0; // 막

    @Builder.Default
    @Column(nullable = false)
    @ColumnDefault("0")
    private Boolean isDelete = false; // 삭제 여부

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
