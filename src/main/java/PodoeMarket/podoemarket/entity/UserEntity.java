package PodoeMarket.podoemarket.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "user")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class UserEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(unique = true, nullable = false)
    private String userId;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(length = 11)
    private String phoneNumber;

    @Column(nullable = false)
    private String nickname;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    @ColumnDefault("0")
    private boolean auth;

    @Column(nullable = false)
    private LocalDate date;

    @PrePersist
    protected void onCreate() {
        date = LocalDate.now(ZoneId.of("Asia/Seoul"));
    }
    @PreUpdate
    protected void onUpdate() {date= LocalDate.now(ZoneId.of("Asia/Seoul"));}

    // user : product_info = 1 : N
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductInfoEntity> product = new ArrayList<>();

    // user : basket = 1 : N
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BasketEntity> basket = new ArrayList<>();

    // user : favorite = 1 : N
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FavoriteEntity> favorite = new ArrayList<>();

    // user : question = 1 : N
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<QEntity> question = new ArrayList<>();

    // user : product_q = 1 : N
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<ProductQEntity> product_q = new ArrayList<>();

    // user : product_review = 1 : N
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<ProductReviewEntity> product_review = new ArrayList<>();

    // user : with_script = 1 : N
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<WishScriptEntity> wish_script = new ArrayList<>();
}