package PodoeMarket.podoemarket.dto.response;

import PodoeMarket.podoemarket.dto.response.ProductListDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DateProductDTO {
    private LocalDate date;
    private List<ProductListDTO> products;
}
