package PodoeMarket.podoemarket.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

@Entity
@Table(name = "qna")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class QnAEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false)
    private String question;

    @Column
    private String answer;

    @Column(nullable = false)
    @ColumnDefault("0")
    private boolean status; // 자주하는 질문

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
