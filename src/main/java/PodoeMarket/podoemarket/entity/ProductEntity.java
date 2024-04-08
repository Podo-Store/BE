package PodoeMarket.podoemarket.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDate;
import java.time.ZoneId;
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
    private int category;
    // 0 : 전체, 1 : 장막극, 2 : 중막극, 3 : 단막극 , 4 : 촌극

    @Column(nullable = false)
    @ColumnDefault("0")
    private int genre;
    // 0 : 전체, .. 추후 논의 필요

    @Column(nullable = false)
    @ColumnDefault("0")
    private int characterNumber;

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
}
