package PodoeMarket.podoemarket.product.dto.request;

import PodoeMarket.podoemarket.common.entity.type.StandardType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewUpdateRequestDTO {
    private Integer rating;
    private StandardType standardType;
    private String content;
}
