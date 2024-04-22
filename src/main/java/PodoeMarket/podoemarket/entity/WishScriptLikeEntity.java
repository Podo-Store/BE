package PodoeMarket.podoemarket.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "wish_script_like")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WishScriptLikeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    // user : wish_script_like = 1 : N
    @ManyToOne(targetEntity = UserEntity.class)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    // wish_script : wish_script_like = 1 : N
    @ManyToOne(targetEntity = WishScriptEntity.class)
    @JoinColumn(name = "wish_script_id", nullable = false)
    private WishScriptEntity wish_script;
}
