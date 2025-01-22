package PodoeMarket.podoemarket.admin.dto.request;

import PodoeMarket.podoemarket.entity.type.PlayType;
import PodoeMarket.podoemarket.entity.type.ProductStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlayTypeRequestDTO {
    private PlayType playType;
    private ProductStatus productStatus;
}
