package PodoeMarket.podoemarket.admin.dto.response;

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
    private Long viewLogCnt;   // 대본 열람 기록 수 (viewCnt는 작품 상세 조회수 합계로 다름)
    private Long accessLogCnt; // 접속 기록 수
}
