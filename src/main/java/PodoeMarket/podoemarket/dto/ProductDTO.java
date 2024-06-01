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
public class ProductDTO {
    private UUID id;
    private String title;
    private String writer;
    private String filePath;
    private int genre;
    private int characterNumber;
    private int runtime;
    private int price;
    private boolean performance;
    private int performancePrice;
    private String content;
    private boolean status;
    private boolean checked;
    private LocalDate date;
}
