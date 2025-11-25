package PodoeMarket.podoemarket.profile.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NicepayCancelResponseDTO {
    private String resultCode;
    private String resultMsg;
    private String tid; // 결제 승인 키
    private String cancelledTid; // 취소 거래 키 (전체취소면 동일, 부분취소는 다를 수 있음)
    private String orderId;
    private String ediDate;          // 응답 전문 생성일시
    private String signature;        // 위변조 검증용 signData
    private String status;           // 상태 (paid/ready/failed/cancelled/partialCancelled/expired)
    private Integer amount;          // 결제 총 금액
    private Integer balanceAmt;      // 취소 가능 잔액
    private String goodsName;        // 상품명
    private String mallReserved;     // 예비필드
    private Boolean useEscrow;
    private String currency;
    private String channel;
    private String paidAt;
    private String failedAt;
    private String cancelledAt;

    // 구매자 정보
    private String buyerName;
    private String buyerTel;
    private String buyerEmail;
    private Boolean issuedCashReceipt;
    private String receiptUrl;
    private String mallUserId;

    // 할인 정보
    private Coupon coupon;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Coupon {
        private Integer couponAmt;
    }

    // 카드 정보
    private Card card;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Card {
        private String cardCode;
        private String cardName;
        private String cardNum;
        private String cardQuota;
        private Boolean isInterestFree;
        private String cardType;
        private String canPartCancel;
        private String acquCardCode;
        private String acquCardName;
    }
}
