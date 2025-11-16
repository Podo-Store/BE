package PodoeMarket.podoemarket.order.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NicepayApproveResponseDTO {
    private String resultCode;   // "0000"이면 승인 성공
    private String resultMsg;
    private String tid;          // 거래 ID
    private String orderId;
    private Integer amount;
    private String payMethod;
    private String authDate;
}
