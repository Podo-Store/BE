package PodoeMarket.podoemarket.profile.dto.response;

import PodoeMarket.podoemarket.common.entity.type.PlayType;
import PodoeMarket.podoemarket.common.entity.type.ProductStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderScriptsResponseDTO {
    private String nickname;
    List<DateScriptOrderResponseDTO> orderList;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DateScriptOrderResponseDTO {
        private LocalDate date;
        private List<OrderScriptDTO> orders;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class OrderScriptDTO {
            private Boolean delete;
            private UUID id;
            private String title;
            private String writer;
            private String imagePath;
            private ProductStatus checked;
            private PlayType playType;
            private Integer performanceAmount;
            private Boolean script;
            private Long scriptPrice;

            private UUID productId;
        }
    }
}
