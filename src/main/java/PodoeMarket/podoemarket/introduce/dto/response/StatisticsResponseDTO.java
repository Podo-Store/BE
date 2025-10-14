package PodoeMarket.podoemarket.introduce.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatisticsResponseDTO {
    private Long userCnt;
    private Long scriptCnt;
    private Long viewCnt;
    private Long reviewCnt;
}
