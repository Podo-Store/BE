package PodoeMarket.podoemarket.product.dto.response;

import PodoeMarket.podoemarket.dto.response.ProductListDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScriptListResponseDTO {
    private List<ProductListDTO> longPlay;
    private List<ProductListDTO> shortPlay;
}
