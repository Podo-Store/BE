package PodoeMarket.podoemarket.dto.response;

import PodoeMarket.podoemarket.entity.type.PlayType;
import PodoeMarket.podoemarket.entity.type.ProductStatus;
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
public class ProductListDTO {
    private UUID id;
    private String title;
    private String writer;
    private String imagePath;
    private boolean script;
    private int scriptPrice;
    private boolean performance;
    private int performancePrice;
    private PlayType playType;
    private ProductStatus checked;
    private LocalDateTime date;
}
