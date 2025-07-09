package PodoeMarket.podoemarket.product.dto.response;

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
public class ReviewResponseDTO {
    private UUID id;
    private String imagePath;
    private String title;
    private String writer;
    private Integer rating;
    private StandardType standardType;
    private String content;
}
