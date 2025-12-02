package PodoeMarket.podoemarket.order.dto.request;

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
    private int paymentMethod;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ApplicantDTO {
        private String name;
        private String phoneNumber;
        private String address;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemDTO {
        private Boolean script;
        private Integer performanceAmount;

        private UUID productId;
    }
}
