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
    private boolean script;
    private int scriptPrice;
    private boolean performance;
    private int performancePrice;
    private String descriptionPath;
    private String plot;
}
