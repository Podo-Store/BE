package PodoeMarket.podoemarket.common.entity;

import PodoeMarket.podoemarket.common.entity.type.SocialLoginType;
import PodoeMarket.podoemarket.common.entity.type.StageType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "user")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(unique = true, nullable = false)
    private String userId;

    @Column(unique = true, nullable = false)
    private String email;

    private String password;

    @Column(nullable = false)
    private String nickname;

    @Builder.Default
    @Column(nullable = false)
    @ColumnDefault("false")
    private boolean auth = false;

    @Enumerated(EnumType.STRING)
    @Column
    private SocialLoginType socialLoginType;

    @Enumerated(EnumType.STRING)
    @Column
    private StageType stageType;

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
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private List<ProductEntity> product = new ArrayList<>();

    // user : order = 1 : N
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<OrdersEntity> order = new ArrayList<>();

    // user : orderItem = 1 : N
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<OrderItemEntity> orderItem = new ArrayList<>();

    // user : refund = 1 : N
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<RefundEntity> refund = new ArrayList<>();

    // user : like = 1 : N
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ProductLikeEntity> like = new ArrayList<>();

    // user : review = 1 : N
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ReviewEntity> review = new ArrayList<>();

    // user : reviewLike = 1 : N
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY
    )
    private List<ReviewLikeEntity> reviewLike = new ArrayList<>();
}