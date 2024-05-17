package PodoeMarket.podoemarket.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WishScriptDTO {
    private UUID id;
    private String content;
    private int genre;
    private int characterNumber;
    private int runtime;
    private LocalDate date;
    private String nickname;
    private String profileFilePath;
    private boolean isLike; // 좋아요 여부
    private int likeCount;
}
