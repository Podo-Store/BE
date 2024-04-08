package PodoeMarket.podoemarket.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

@Entity
@Table(name = "product_q")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProductQEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String content;

    @Column(nullable = false)
    private boolean open;

    @Column(nullable = false)
    private LocalDate date;

    @PrePersist
    protected void onCreate() {
        date = LocalDate.now(ZoneId.of("Asia/Seoul"));
    }
    @PreUpdate
    protected void onUpdate() {date= LocalDate.now(ZoneId.of("Asia/Seoul"));}

    // product_q : product_a = 1 : 1
    @OneToOne(mappedBy = "product_q", cascade = CascadeType.ALL, orphanRemoval = true)
    private ProductAEntity product_a;
}
