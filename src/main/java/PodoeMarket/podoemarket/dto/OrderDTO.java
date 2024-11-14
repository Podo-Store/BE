package PodoeMarket.podoemarket.dto;

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
public class OrderDTO {
    ApplicantDTO applicant;
    List<OrderItemDTO> orderItem;
    private int totalPrice;
    private int paymentMethod;
}
