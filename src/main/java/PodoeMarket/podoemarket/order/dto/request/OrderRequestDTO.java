package PodoeMarket.podoemarket.order.dto.request;

import PodoeMarket.podoemarket.common.entity.type.PlayType;
import PodoeMarket.podoemarket.dto.ApplicantDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderRequestDTO {
    ApplicantDTO applicant;
    List<OrderItemDTO> orderItem;
    private int totalPrice;
    private int paymentMethod;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemDTO {
        private boolean delete;
        private UUID id;
        private String title;
        private String writer;
        private String imagePath;
        private PlayType playType;
        private boolean script;
        private int scriptPrice;
        private int performanceAmount;
        private int performancePrice;
        private int performanceTotalPrice;
        private int possibleCount;
        private int totalPrice;

        private UUID productId;
    }
}
