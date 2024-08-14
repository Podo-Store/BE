package PodoeMarket.podoemarket.dto;

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
