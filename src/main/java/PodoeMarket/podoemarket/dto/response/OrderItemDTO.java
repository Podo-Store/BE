package PodoeMarket.podoemarket.dto.response;

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
    private boolean delete;
    private UUID id;
    private String title;
    private String writer;
    private String imagePath;
    private boolean checked;
    private int playType;
    private boolean script;
    private int scriptPrice;
    private int performanceAmount;
    private int performancePrice;
    private int contractStatus; // 0: 공연권 판매 안함, 1: 공연권 구매, 2: 계약 완료
    private int totalPrice;

    private UUID productId;
}