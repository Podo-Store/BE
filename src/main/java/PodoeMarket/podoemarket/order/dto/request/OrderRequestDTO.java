package PodoeMarket.podoemarket.order.dto.request;

import PodoeMarket.podoemarket.dto.ApplicantDTO;
import PodoeMarket.podoemarket.dto.response.OrderItemDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderRequestDTO {
    ApplicantDTO applicant;
    List<OrderItemDTO> orderItem;
    private int totalPrice;
    private int paymentMethod;
}
