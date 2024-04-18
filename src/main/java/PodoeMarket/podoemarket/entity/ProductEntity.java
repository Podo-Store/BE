package PodoeMarket.podoemarket.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDate;
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

    @Column
    private String title;

    @Column
    private String writer;

    @Column
    private String file;

    @Column(nullable = false)
    @ColumnDefault("0")
    private int genre;
    // (1부터 10까지 순서대로)
    // 로맨스, 스릴러, 코미디, 드라마, 모험, 미스터리, SF, 공포, 판타지, 시대극

    @Column(nullable = false)
    @ColumnDefault("0")
    private int characterNumber;
    // 1,2,3,4,5,6,7인 이상

    @Column(nullable = false)
    @ColumnDefault("0")
    private int runtime;
    // 1 : 30분 이내, 2 : 1시간, 3 : 1시간 30분, 4 : 2시간 이상

    // 관리자(심사 주체) 확인 여부
    @Column(nullable = false)
    @ColumnDefault("0")
    private boolean checked;

    @Column(nullable = false)
    private LocalDate date;

    @PrePersist // entity가 영속화되기 직전에 실행
    protected void onCreate() {
        date = LocalDate.now(ZoneId.of("Asia/Seoul"));
    }
    @PreUpdate // db에 entity가 업데이트되기 직전에 실행
    protected void onUpdate() {date= LocalDate.now(ZoneId.of("Asia/Seoul"));}

    // product : product_info = 1 : 1
    @OneToOne(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = false)
    private ProductInfoEntity product_info;

    // product : product_review = 1 : N
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL)
    private List<ProductReviewEntity> product_review = new ArrayList<>();
}
