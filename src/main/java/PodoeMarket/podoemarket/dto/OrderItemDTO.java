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
    private boolean delete;
    private UUID id;
    private String title;
    private String writer;
    private String imagePath;
    private boolean checked;
    private int playType;
    private boolean script;
    private int scriptPrice;
    private boolean performance;
    private int performancePrice;
    private int contractStatus; // 0: 공연권 판매 안함, 1: 공연권 구매, 2: 계약 완료
    private int totalPrice;
    private int buyPerformance; // 0: 구매 불가, 1: 계약 필요, 2: 계약 중, 3: 구매 가능

    private UUID productId;
}