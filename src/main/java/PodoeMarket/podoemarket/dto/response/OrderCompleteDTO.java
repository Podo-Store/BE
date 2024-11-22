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
public class OrderCompleteDTO {
    private Long Id;
    private LocalDateTime orderDate;
    private Long orderNum;
    private String title;
    private int scriptPrice;
    private int performancePrice;
    private int performanceAmount;
    private int totalPrice;
}
