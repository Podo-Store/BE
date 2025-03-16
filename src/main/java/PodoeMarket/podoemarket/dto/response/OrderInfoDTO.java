package PodoeMarket.podoemarket.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderInfoDTO {
    private LocalDateTime orderDate;
    private Long orderId;
    private String title;
    private boolean script;
    private int scriptPrice;
    private int performanceAmount;
    private int performancePrice;
}
