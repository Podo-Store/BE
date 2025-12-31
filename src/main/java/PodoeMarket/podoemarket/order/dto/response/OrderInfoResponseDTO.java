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
public class OrderInfoResponseDTO {
    private LocalDateTime orderDate;
    private String pgOrderId;
    private String title;
    private Boolean script;
    private Long scriptPrice;
    private Integer performanceAmount;
    private Long performancePrice;
}
