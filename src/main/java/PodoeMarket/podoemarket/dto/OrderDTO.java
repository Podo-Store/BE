package PodoeMarket.podoemarket.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDTO {
    List<OrderItemDTO> orderItem;
    private int status; // 0: 결제 전, 1: 결제 완료
    private int totalPrice;
}
