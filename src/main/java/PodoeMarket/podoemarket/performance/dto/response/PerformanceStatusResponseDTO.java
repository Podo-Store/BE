package PodoeMarket.podoemarket.performance.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PerformanceStatusResponseDTO {
    private List<PerformanceListDTO> ongoing;
    private List<PerformanceListDTO> upcoming;
    private List<PerformanceListDTO> past;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PerformanceListDTO {
        private UUID id;
        private String posterPath;
        private String title;
        private String place;
        private LocalDate startDate;
        private LocalDate endDate;
        private Boolean isUsed;
        private Boolean isOwner;
        private String link;
    }
}
