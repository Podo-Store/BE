package PodoeMarket.podoemarket.admin.dto.request;

import PodoeMarket.podoemarket.common.entity.type.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentStatusRequestDTO {
    private OrderStatus orderStatus;
}
