package PodoeMarket.podoemarket.common.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "performanceDate")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PerformanceDateEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column
    private LocalDateTime date;

    // orderItem : performanceDate = 1 : N
    @ManyToOne(targetEntity = OrderItemEntity.class)
    @JoinColumn(name  = "orderItem_id", nullable = false)
    private OrderItemEntity orderItem;
}
