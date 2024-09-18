package PodoeMarket.podoemarket.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplyDTO {
    private String filePath;
    private String title;
    private String writer;
    private int performanceAmount;
    private ApplicantDTO applicant;
    private PerformanceDateDTO performanceDate;
}
