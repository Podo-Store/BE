package PodoeMarket.podoemarket.product.dto.request;

import PodoeMarket.podoemarket.common.entity.type.StandardType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewRequestDTO {
    private UUID productId;
    private Integer rating;
    private StandardType standardType;
    private String content;
}
