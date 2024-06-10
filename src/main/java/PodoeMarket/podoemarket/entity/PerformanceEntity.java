package PodoeMarket.podoemarket.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "performance")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PerformanceEntity {
    // 수정 진행 중!
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column
    private String link;

    @Column
    private String posterPath;

    @Column
    private String posterType;
}
