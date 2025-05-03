package PodoeMarket.podoemarket.product.dto.response;

import PodoeMarket.podoemarket.common.entity.type.PlayType;
import PodoeMarket.podoemarket.common.entity.type.ProductStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScriptDetailResponseDTO {
    private UUID id;
    private String title;
    private String writer;
    private String imagePath;
    private Boolean script;
    private Integer scriptPrice;
    private Boolean performance;
    private Integer performancePrice;
    private String descriptionPath;
    private PlayType playType;
    private ProductStatus checked;
    private String plot;
    private LocalDateTime date;

    // 옵션 선택 드롭다운
    // 0 : 아무것도 구매 X
    // 1 : 대본 or 대본 + 공연권 (대본 권리 기간 유효 시)
    // 2 : 공연권만 보유
    private Integer buyStatus;
    private Boolean like;
    private Integer likeCount;
    private Long viewCount;

    // 개요
    private Integer any;
    private Integer male;
    private Integer female;
    private String stageComment;
    private Integer runningTime;
    private Integer scene; // 장
    private Integer act; // 막
}
