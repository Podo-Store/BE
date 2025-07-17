package PodoeMarket.podoemarket.product.dto.response;

import PodoeMarket.podoemarket.common.entity.type.PlayType;
import PodoeMarket.podoemarket.common.entity.type.ProductStatus;
import PodoeMarket.podoemarket.common.entity.type.StageType;
import PodoeMarket.podoemarket.common.entity.type.StandardType;
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
public class ScriptDetailResponseDTO {
    private UUID id;
    private String title;
    private String writer;
    private String imagePath;
    private Boolean script;
    private Long scriptPrice;
    private Boolean performance;
    private Long performancePrice;
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
    private Long likeCount;
    private Long viewCount;
    private String intention;

    // 개요
    private Integer any;
    private Integer male;
    private Integer female;
    private String stageComment;
    private Integer runningTime;
    private Integer scene; // 장
    private Integer act; // 막

    private Boolean isReviewWritten; // 후기 작성 여부

    // 후기
    private ReviewStatisticsDTO reviewStatistics;

    // 후기 리스트
    private List<ReviewListResponseDTO> reviews;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReviewStatisticsDTO {
        private Integer totalReviewCount;
        private Integer totalReviewPages;
        private Double reviewAverageRating;
        private Double fiveStarPercent;
        private Double fourStarPercent;
        private Double threeStarPercent;
        private Double twoStarPercent;
        private Double oneStarPercent;
        private Double characterPercent;
        private Double relationPercent;
        private Double storyPercent;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReviewListResponseDTO {
        private UUID id;
        private String nickname;
        private StageType stageType;
        private LocalDateTime date;
        private Boolean isEdited; // 수정 여부
        private Boolean myself; // 본인 작성 여부
        private Integer rating;
        private StandardType standardType;
        private String content;
        private Boolean isLike;
        private Long likeCount;
    }

}
