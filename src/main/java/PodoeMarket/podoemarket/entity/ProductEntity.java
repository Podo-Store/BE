package PodoeMarket.podoemarket.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "product")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProductEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "JSON")
    private String file;

    @Column(nullable = false)
    private int category;
    // 0 : 전체, 1 : 장막극, 2 : 중막극, 3 : 단막극 , 4 : 촌극

    @Column(nullable = false)
    private int genre;
    // 0 : 전체, .. 추후 논의 필요

    @Column(nullable = false)
    private int price;

    @Column(nullable = false)
    private boolean performance;

    @Column
    private int performancePrice;

    @Column(nullable = false)
    private String content;

    @Column(nullable = false)
    private int status;
    // 0 : 판매 중, 1 : 판매 중단

    // user : product = 1 : N

    // product : basket = 1 : N

    // product : like = 1 : N
}
