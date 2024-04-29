package PodoeMarket.podoemarket.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "script")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ScriptEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column
    private String title;

    @Column
    private String type;

    @Column
    private String filePath;
}

