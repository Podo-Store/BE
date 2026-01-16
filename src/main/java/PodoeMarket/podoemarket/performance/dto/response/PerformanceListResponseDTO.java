package PodoeMarket.podoemarket.performance.dto.response;

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
public class PerformanceListResponseDTO {
    private UUID id;
    private String posterPath;
    private String title;
    private String place;
    private LocalDate startDate;
    private LocalDate endDate;
    private Boolean isUsed;
}
