package PodoeMarket.podoemarket.order.dto.response;

import PodoeMarket.podoemarket.common.entity.type.PlayType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemResponseDTO {
    private UUID productId;
    private String title;
    private String writer;
    private String imagePath;
    private PlayType playType;
    private boolean script;
    private int scriptPrice;
    private int performanceAmount;
    private int performancePrice;
    private int performanceTotalPrice;
    private int totalPrice;
}
