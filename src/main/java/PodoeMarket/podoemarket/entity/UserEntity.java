package PodoeMarket.podoemarket.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "user")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class UserEntity implements UserDetails { // UserDetails를 상속받아 인증 객체로 사용
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(unique = true, nullable = false)
    private String userId;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, length = 11)
    private String phoneNumber;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String nickname;

    @Column(nullable = false)
    @ColumnDefault("0")
    private boolean auth;

    // user : product = 1 : N
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductEntity> product = new ArrayList<>();

    // user : basket = 1 : N
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BasketEntity> basket = new ArrayList<>();

    // user : favorite = 1 : N
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FavoriteEntity> favorite = new ArrayList<>();

    @Builder
    public UserEntity(String userId, String password, String auth) {
        this.userId = userId;
        this.password = password;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("user"));
    }

    @Override
    public String getUsername() {
        return userId;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}