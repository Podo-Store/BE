package PodoeMarket.podoemarket.profile.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundResponseDTO {
    private String scriptImage;
    private String title;
    private String writer;
    private Long performancePrice;
    private LocalDateTime orderDate;
    private Long orderNum;
    private Integer orderAmount;
    private Long orderPrice;
    private Integer possibleAmount;
    private Long possiblePrice;
}
