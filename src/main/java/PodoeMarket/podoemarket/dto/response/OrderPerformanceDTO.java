package PodoeMarket.podoemarket.dto.response;

import PodoeMarket.podoemarket.entity.type.PlayType;
import PodoeMarket.podoemarket.entity.type.ProductStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderPerformanceDTO {
    private boolean delete;
    private UUID id;
    private String title;
    private String writer;
    private String imagePath;
    private ProductStatus checked;
    private PlayType playType;
    private int performanceAmount;
    private int performancePrice;
    private int performanceTotalPrice;
    private int possibleCount;

    private UUID productId;
    private boolean paymentStatus;
}
