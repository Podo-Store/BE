package PodoeMarket.podoemarket.product.dto.response;

import PodoeMarket.podoemarket.common.entity.type.ProductStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScriptListResponseDTO {
    private List<ProductListDTO> longPlay;
    private List<ProductListDTO> shortPlay;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductListDTO {
        private UUID id;
        private String title;
        private String writer;
        private String imagePath;
        private Boolean script;
        private Long scriptPrice;
        private Boolean performance;
        private Long performancePrice;
        private ProductStatus checked;
        private LocalDateTime date;
        private Boolean like;
        private Long likeCount;
        private Long viewCount;
    }
}
