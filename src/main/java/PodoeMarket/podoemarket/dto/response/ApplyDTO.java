package PodoeMarket.podoemarket.dto.response;

import PodoeMarket.podoemarket.dto.ApplicantDTO;
import PodoeMarket.podoemarket.dto.PerformanceDateDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplyDTO {
    private UUID orderItemId;
    private String ImagePath;
    private String title;
    private String writer;
    private int performanceAmount;
    private ApplicantDTO applicant;
    List<PerformanceDateDTO> performanceDate;
}
