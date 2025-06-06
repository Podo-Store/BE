package PodoeMarket.podoemarket.profile.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestedPerformanceResponseDTO {
    private ProductInfo productInfo;
    private List<DateRequestedList> dateRequestedList;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductInfo {
        private String imagePath;
        private String title;
        private String writer;
        private String plot;
        private Boolean script;
        private Long scriptPrice;
        private Integer scriptQuantity;
        private Boolean performance;
        private Long performancePrice;
        private Integer performanceQuantity;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DateRequestedList {
        private LocalDate date;
        private List<ApplicantInfo> requestedInfo;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ApplicantInfo {
        private Integer amount;
        private String name;
        private String phoneNumber;
        private String address;
        private List<PerformanceDate> performanceDateList;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PerformanceDate {
        private LocalDateTime date;
    }
}
