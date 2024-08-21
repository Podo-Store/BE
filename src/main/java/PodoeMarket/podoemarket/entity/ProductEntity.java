package PodoeMarket.podoemarket.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDate;
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
    @ColumnDefault("False")
    private boolean script; // 대본권 판매 여부

    @Column(nullable = false)
    @ColumnDefault("0")
    private int scriptPrice;

    @Column(nullable = false)
    @ColumnDefault("False")
    private boolean performance; // 공연권 판매 여부

    @Column
    @ColumnDefault("0")
    private int performancePrice;

    @Column
    private String descriptionPath;
    
    @Column
    @ColumnDefault("0")
    private int playType; // 0: default 1: 장편극, 2: 단편극

    // 관리자(심사 주체) 확인 여부
    @Column(nullable = false)
    @ColumnDefault("0")
    private boolean checked;

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
    @ManyToOne(targetEntity = UserEntity.class)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private UserEntity user;

    // product : orderItem = 1 : N
    @OneToMany(mappedBy = "product")
    @JsonIgnore
    private List<OrderItemEntity> orderItem = new ArrayList<>();
}
