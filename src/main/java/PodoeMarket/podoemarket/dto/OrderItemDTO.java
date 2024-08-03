package PodoeMarket.podoemarket.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemDTO {
    private UUID id;
    private String title;
    private String writer;
    private String imagePath;
    private boolean checked;
    private boolean script;
    private int scriptPrice;
    private boolean performance;
    private int performancePrice;
    private int totalPrice;

    private UUID productId;
}
