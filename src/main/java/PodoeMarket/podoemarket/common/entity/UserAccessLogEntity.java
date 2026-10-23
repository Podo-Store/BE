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
        name = "userAccessLog",
        indexes = {
                @Index(name = "idx_user_access_log_user", columnList = "user_id, accessed_at"),
                @Index(name = "idx_user_access_log_accessed", columnList = "accessed_at")
        }
)
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserAccessLogEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "accessed_at", nullable = false)
    private LocalDateTime accessedAt;
}
