package PodoeMarket.podoemarket.profile.dto;

import PodoeMarket.podoemarket.common.entity.type.ProductStatus;
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
public class WorkListResponseDTO {
    private String nickname;
    private List<DateWorkDTO> dateWorks;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DateWorkDTO {
        private LocalDate date;
        private List<WorksDTO> works;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class WorksDTO {
            private UUID id;
            private String title;
            private String imagePath;
            private boolean script;
            private int scriptPrice;
            private boolean performance;
            private int performancePrice;
            private ProductStatus checked;
        }
    }
}
