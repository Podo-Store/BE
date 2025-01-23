package PodoeMarket.podoemarket.admin.dto.response;

import PodoeMarket.podoemarket.common.entity.type.PlayType;
import PodoeMarket.podoemarket.common.entity.type.ProductStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
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
public class ProductManagementResponseDTO {
    private Long passCnt;
    private Long waitCnt;
    private Long productCnt;
    private List<ProductDTO> products;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductDTO {
        private UUID id;
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        private LocalDateTime createdAt;
        private String title;
        private String writer;
        private ProductStatus checked;
        private PlayType playType;
    }
}