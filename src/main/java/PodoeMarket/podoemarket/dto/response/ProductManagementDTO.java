package PodoeMarket.podoemarket.dto.response;

import PodoeMarket.podoemarket.entity.type.PlayType;
import PodoeMarket.podoemarket.entity.type.ProductStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.parameters.P;

import java.util.Date;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductManagementDTO {
    private Long passCnt;
    private Long waitCnt;
    private Long productCnt;
    private List<ProductDTO> products;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductDTO {
        private Long id;
        private Date createdAt;
        private String title;
        private String writer;
        private ProductStatus checked;
        private PlayType playType;
    }
}