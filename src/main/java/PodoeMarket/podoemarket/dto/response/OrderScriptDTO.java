package PodoeMarket.podoemarket.dto.response;

import PodoeMarket.podoemarket.common.entity.type.PlayType;
import PodoeMarket.podoemarket.common.entity.type.ProductStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderScriptDTO {
    private boolean delete;
    private UUID id;
    private String title;
    private String writer;
    private String imagePath;
    private ProductStatus checked;
    private PlayType playType;
    private int performanceAmount;
    private boolean script;
    private int scriptPrice;

    private UUID productId;
    private boolean paymentStatus;
}
