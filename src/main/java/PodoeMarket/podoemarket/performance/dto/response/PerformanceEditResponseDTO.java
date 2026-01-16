package PodoeMarket.podoemarket.performance.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PerformanceEditResponseDTO {
    private String posterPath;
    private String title;
    private String place;
    private LocalDate startDate;
    private LocalDate endDate;
    private String link;
    private Boolean isUsed;
}
