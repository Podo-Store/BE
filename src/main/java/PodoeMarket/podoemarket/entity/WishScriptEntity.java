package PodoeMarket.podoemarket.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "wish_script")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WishScriptEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(length = 2024)
    private String content;

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

    @Column(nullable = false)
    private LocalDate date;

    @PrePersist // entity가 영속화되기 직전에 실행
    protected void onCreate() {
        date = LocalDate.now(ZoneId.of("Asia/Seoul"));
    }
    @PreUpdate // db에 entity가 업데이트되기 직전에 실행
    protected void onUpdate() {date= LocalDate.now(ZoneId.of("Asia/Seoul"));}

    // user : with_script = 1 : N
    @ManyToOne(targetEntity = UserEntity.class, fetch = FetchType.LAZY)
    @JoinColumn(name="user_id", nullable = false)
    private UserEntity user;

    // wish_script : wish_script_like = 1 : N
    @OneToMany(mappedBy = "wish_script", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WishScriptLikeEntity> wish_script_like = new ArrayList<>();
}
