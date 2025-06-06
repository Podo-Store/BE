package PodoeMarket.podoemarket.profile.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplyResponseDTO {
    private UUID orderItemId;
    private String ImagePath;
    private String title;
    private String writer;
    private Integer performanceAmount;
    private ApplicantDTO applicant;
    List<PerformanceDateDTO> performanceDate;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ApplicantDTO {
        private String name;
        private String phoneNumber;
        private String address;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PerformanceDateDTO {
        private LocalDateTime date;
    }
}
