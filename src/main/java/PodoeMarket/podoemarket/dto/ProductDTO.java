package PodoeMarket.podoemarket.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.core.io.InputStreamResource;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductDTO {
    private UUID id;
    private String title;
    private String writer;
//    private String filePath;
    private InputStreamResource filePath;
    private String imagePath;
    private boolean script;
    private int scriptPrice;
    private boolean performance;
    private int performancePrice;
    private String descriptionPath;
    private int playType;
    private boolean checked;
    private LocalDateTime date;

    private boolean buyScript; // 대본 구매 여부
}
