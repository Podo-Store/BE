package PodoeMarket.podoemarket.common.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "pdfDownloadLog",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"order_item_id"})
        }
)
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PdfDownloadLogEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_item_id", nullable = false)
    private UUID orderItemId;

    @Column(name = "user_id",nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private LocalDateTime downloadedAt;
}
