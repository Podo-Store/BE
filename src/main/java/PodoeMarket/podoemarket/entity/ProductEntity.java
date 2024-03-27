package PodoeMarket.podoemarket.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.util.Date;
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

    @Column(columnDefinition = "JSON")
    private String file;

    @Column
    private int category;
    // 0 : 전체, 1 : 장막극, 2 : 중막극, 3 : 단막극 , 4 : 촌극

    @Column
    private int genre;
    // 0 : 전체, .. 추후 논의 필요

    // 관리자(심사 주체) 확인 여부
    @Column(nullable = false)
    @ColumnDefault("0")
    private boolean checked;

    // CreatedAt과 UpdatedAt 필드 추가
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_at", nullable = false, updatable = false)
    @CreatedDate
    private Date createdAt;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "updated_at", nullable = false)
    @LastModifiedDate
    private Date updatedAt;

    // product : product_info = 1 : 1
    @OneToOne(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = false)
    private ProductInfoEntity product_info;
}
