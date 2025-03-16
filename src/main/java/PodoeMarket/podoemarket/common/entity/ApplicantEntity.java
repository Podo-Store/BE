package PodoeMarket.podoemarket.common.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "applicant")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ApplicantEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column
    private String name;

    @Column
    private String phoneNumber;

    @Column
    private String address;

    // orderItem : applicant = 1 : 1
    @OneToOne(targetEntity = OrderItemEntity.class)
    @JoinColumn(name = "orderItem_id")
    private OrderItemEntity orderItem;
}
