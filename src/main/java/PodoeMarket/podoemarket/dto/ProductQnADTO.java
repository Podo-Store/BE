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
public class ProductQnADTO {
    private UUID id;
    private String title;
    private String question;
    private String answer;
    private LocalDate date;
    private UUID productId;

    private String userNickname;
}
