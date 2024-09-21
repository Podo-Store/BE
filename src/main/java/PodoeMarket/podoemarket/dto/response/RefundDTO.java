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
public class RefundDTO {
    private String scriptImage;
    private String title;
    private String writer;
    private LocalDateTime orderDate;
    private Long orderNum;
    private int orderAmount;
    private int orderPrice;
    private int possibleAmount;
    private int possiblePrice;
    private int refundAmount;
    private int refundPrice;
    private String reason;
}
