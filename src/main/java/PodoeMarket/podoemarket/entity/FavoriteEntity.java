package PodoeMarket.podoemarket.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "favorite")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FavoriteEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    // 회원 시퀀스 아이디 uuid
    // 작품 시퀀스 아이디 int

    // user : like = 1 : N

    // product : like = 1 : N
}
