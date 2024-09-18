package PodoeMarket.podoemarket.dto.response;

import PodoeMarket.podoemarket.dto.ApplicantDTO;
import PodoeMarket.podoemarket.dto.PerformanceDateDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

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
    List<PerformanceDateDTO> performanceDate;
}
