package PodoeMarket.podoemarket.admin.dto.response;

import PodoeMarket.podoemarket.common.entity.type.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderManagementResponseDTO {
    private Long doneCnt;
    private Long waitingCnt;
    private Long orderCnt;
    private List<OrderDTO> orders;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderDTO {
        private Long id;
        private LocalDateTime orderDate;
        private String title;
        private String writer;
        private String customer;
        private OrderStatus orderStatus;
        private Boolean script; // 대본은 한 번에 1개만 구매 가능
        private Integer performanceAmount;
        private Long totalPrice;
    }
}
