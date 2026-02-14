package PodoeMarket.podoemarket.profile.dto.response;

import PodoeMarket.podoemarket.common.entity.type.OrderStatus;
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
public class OrderPerformanceResponseDTO {
    private String nickname;
    List<DatePerformanceOrderDTO> orderList;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DatePerformanceOrderDTO {
        private LocalDate date;
        private List<OrderPerformanceDTO> orders;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class OrderPerformanceDTO {
            private Boolean delete;
            private UUID id;
            private String title;
            private String writer;
            private String imagePath;
            private ProductStatus checked;
            private PlayType playType;
            private Long performancePrice;
            private Long performanceTotalPrice;
            private Integer possibleCount;

            private UUID productId;
        }
    }
}
