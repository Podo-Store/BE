package PodoeMarket.podoemarket.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "performnace")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PerformanceEntity {
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
