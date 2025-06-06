package PodoeMarket.podoemarket.order.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCompleteResponseDTO {
    private Long id;
    private LocalDateTime orderDate;
    private Long orderNum;
    private String title;
    private Long scriptPrice;
    private Long performancePrice;
    private Integer performanceAmount;
    private Long totalPrice;
}
