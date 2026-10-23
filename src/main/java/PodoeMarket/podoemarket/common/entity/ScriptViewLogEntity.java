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
        name = "scriptViewLog",
        indexes = {
                @Index(name = "idx_script_view_log_product", columnList = "product_id, viewed_at"),
                @Index(name = "idx_script_view_log_user", columnList = "user_id, viewed_at")
        }
)
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ScriptViewLogEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "viewed_at", nullable = false)
    private LocalDateTime viewedAt;
}
