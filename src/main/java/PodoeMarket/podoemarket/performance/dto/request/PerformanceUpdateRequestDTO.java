package PodoeMarket.podoemarket.performance.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PerformanceUpdateRequestDTO {
    private String title;
    private String place;
    private LocalDate startDate;
    private LocalDate endDate;
    private String link;
    private Boolean isUsed;
}
