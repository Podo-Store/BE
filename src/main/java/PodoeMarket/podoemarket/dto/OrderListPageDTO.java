package PodoeMarket.podoemarket.dto;

import PodoeMarket.podoemarket.dto.response.DateOrderDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderListPageDTO {
    private String nickname;
    List<DateOrderDTO> orderList;
}
