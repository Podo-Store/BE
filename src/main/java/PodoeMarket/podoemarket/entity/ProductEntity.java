package PodoeMarket.podoemarket.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.util.ArrayList;
import java.util.Date;
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

    @Column(columnDefinition = "JSON")
    private String file;

    @Column(nullable = false)
    private int category;
    // 0 : 전체, 1 : 장막극, 2 : 중막극, 3 : 단막극 , 4 : 촌극

    @Column(nullable = false)
    private int genre;
    // 0 : 전체, .. 추후 논의 필요

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

    // CreatedAt과 UpdatedAt 필드 추가
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_at", nullable = false, updatable = false)
    @CreatedDate
    private Date createdAt;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "updated_at", nullable = false)
    @LastModifiedDate
    private Date updatedAt;

    // user : product = 1 : N
    @ManyToOne(targetEntity = UserEntity.class)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    // product : basket = 1 : N
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BasketEntity> basket = new ArrayList<>();

    // product : favorite = 1 : N
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FavoriteEntity> favorite = new ArrayList<>();
}
