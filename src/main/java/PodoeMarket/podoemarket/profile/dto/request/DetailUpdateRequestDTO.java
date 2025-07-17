package PodoeMarket.podoemarket.profile.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DetailUpdateRequestDTO {
    private UUID id;
    private String title;
    private String imagePath;
    private Boolean script;
    private Long scriptPrice;
    private Boolean performance;
    private Long performancePrice;
    private String descriptionPath;
    private String plot;
    private String intention;

    // 개요
    private Integer any;
    private Integer male;
    private Integer female;
    private String stageComment;
    private Integer runningTime;
    private Integer scene; // 장
    private Integer act; // 막
}
