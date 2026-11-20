package PodoeMarket.podoemarket.admin.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ViewLogResponseDTO {
    private Long id;
    private UUID userId;
    private String nickname;
    private String email;
    private UUID productId;
    private String title;
    private LocalDateTime viewedAt;
}
