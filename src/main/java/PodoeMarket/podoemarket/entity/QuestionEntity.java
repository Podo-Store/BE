package PodoeMarket.podoemarket.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

@Entity
@Table(name = "question")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class QuestionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false)
    private String content;

    @Column(nullable = false)
    private LocalDate date;

    @PrePersist
    protected void onCreate() {
        date = LocalDate.now(ZoneId.of("Asia/Seoul"));
    }
    @PreUpdate
    protected void onUpdate() {date= LocalDate.now(ZoneId.of("Asia/Seoul"));}


    // user : question = 1 : N
    @ManyToOne(targetEntity = UserEntity.class)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;
}
