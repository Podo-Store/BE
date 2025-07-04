package PodoeMarket.podoemarket.product.dto.response;

import PodoeMarket.podoemarket.common.entity.type.StandardType;
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
public class ReviewListResponseDTO {
    private UUID id;
    private String nickname;
    private LocalDateTime date;
    private Boolean myself; // 본인 작성 여부
    private Integer rating;
    private StandardType standardType;
    private String content;
    private Boolean isLike;
    private Long likeCount;
}
