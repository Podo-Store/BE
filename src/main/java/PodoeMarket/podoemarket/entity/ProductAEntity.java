package PodoeMarket.podoemarket.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

@Entity
@Table(name = "product_a")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProductAEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column
    private String content;

    @Column(nullable = false)
    private LocalDate date;

    @PrePersist // entity가 영속화되기 직전에 실행
    protected void onCreate() {
        date = LocalDate.now(ZoneId.of("Asia/Seoul"));
    }
    @PreUpdate // db에 entity가 업데이트되기 직전에 실행
    protected void onUpdate() {date= LocalDate.now(ZoneId.of("Asia/Seoul"));}

    // product_q : product_a = 1 : 1
    @OneToOne
    private ProductQEntity product_q;
}
