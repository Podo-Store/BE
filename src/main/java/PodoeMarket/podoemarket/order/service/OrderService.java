package PodoeMarket.podoemarket.order.service;

import PodoeMarket.podoemarket.common.entity.*;
import PodoeMarket.podoemarket.common.entity.type.OrderStatus;
import PodoeMarket.podoemarket.order.dto.request.OrderInfoRequestDTO;
import PodoeMarket.podoemarket.order.dto.response.NicepayApproveResponseDTO;
import PodoeMarket.podoemarket.order.dto.response.OrderCompleteResponseDTO;
import PodoeMarket.podoemarket.order.dto.request.OrderRequestDTO;
import PodoeMarket.podoemarket.common.repository.ApplicantRepository;
import PodoeMarket.podoemarket.common.repository.OrderItemRepository;
import PodoeMarket.podoemarket.common.repository.OrderRepository;
import PodoeMarket.podoemarket.common.repository.ProductRepository;
import PodoeMarket.podoemarket.order.dto.response.OrderInfoResponseDTO;
import PodoeMarket.podoemarket.order.dto.response.OrderItemResponseDTO;
import PodoeMarket.podoemarket.service.MailSendService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RequiredArgsConstructor
@Slf4j
@Service
@Transactional(readOnly = true)
public class OrderService {
    private final ProductRepository productRepo;
    private final OrderRepository orderRepo;
    private final OrderItemRepository orderItemRepo;
    private final ApplicantRepository applicantRepo;
    private final MailSendService mailSendService;

    @Value("${cloud.aws.s3.url}")
    private String bucketURL;

    @Value("${nicepay.client-id}")
    private String clientId;

    @Value("${nicepay.secret-key}")
    private String secretKey;

    public OrderItemResponseDTO getOrderItemInfo(UserEntity userInfo, OrderInfoRequestDTO dto) {
        try {
            final ProductEntity orderProduct = getProduct(dto.getProductId());

            if(orderProduct == null)
                throw new RuntimeException("작품 정보 조회 실패");

            if(userInfo.getId().equals(orderProduct.getUser().getId()))
                throw new RuntimeException("본인 작품 구매 불가");

            final long totalPrice = (dto.getScript() ? orderProduct.getScriptPrice() : 0) + (dto.getPerformanceAmount() > 0 ? orderProduct.getPerformancePrice() * dto.getPerformanceAmount() : 0);
            final String encodedScriptImage = orderProduct.getImagePath() != null ? bucketURL + URLEncoder.encode(orderProduct.getImagePath(), StandardCharsets.UTF_8) : "";

            return OrderItemResponseDTO.builder()
                    .title(orderProduct.getTitle())
                    .writer(orderProduct.getWriter())
                    .imagePath(encodedScriptImage)
                    .playType(orderProduct.getPlayType())
                    .script(dto.getScript())
                    .scriptPrice(dto.getScript() ? orderProduct.getScriptPrice() : 0)
                    .performanceAmount(dto.getPerformanceAmount())
                    .performancePrice(orderProduct.getPerformancePrice())
                    .performanceTotalPrice(dto.getPerformanceAmount() > 0 ? orderProduct.getPerformancePrice() * dto.getPerformanceAmount() : 0)
                    .totalPrice(totalPrice)
                    .build();
        } catch (Exception e) {
            throw e;
        }
    }

    @Transactional
    public List<OrderCompleteResponseDTO> purchaseProduct(UserEntity userInfo, OrderRequestDTO dto) {
        try {
            final OrdersEntity order = OrdersEntity.builder()
                    .user(userInfo)
                    .paymentMethod(dto.getPaymentMethod())
                    .orderStatus(OrderStatus.WAIT)
                    .build();

            final OrdersEntity orders = orderCreate(order, dto, userInfo);

            if (buyPerformance(orders.getId())) {
                final ApplicantEntity applicant = ApplicantEntity.builder()
                        .name(dto.getApplicant().getName())
                        .phoneNumber(dto.getApplicant().getPhoneNumber())
                        .address(dto.getApplicant().getAddress())
                        .orderItem(orders.getOrderItem().getFirst())
                        .build();

                createApplicant(applicant);
            }

            return orderResult(orders);
        } catch (Exception e) {
            throw e;
        }
    }

    @Transactional
    public OrderInfoResponseDTO orderSuccess(Long orderId) {
        try {
            final OrdersEntity order = getOrderInfo(orderId);
            final List<OrderItemEntity> orderItem = getOrderItem(orderId);

            OrderInfoResponseDTO orderInfo = OrderInfoResponseDTO.builder()
                    .orderId(orderId)
                    .orderDate(order.getCreatedAt())
                    .title(orderItem.getFirst().getProduct().getTitle())
                    .script(orderItem.getFirst().getScript())
                    .scriptPrice(orderItem.getFirst().getScriptPrice())
                    .performanceAmount(orderItem.getFirst().getPerformanceAmount())
                    .performancePrice(orderItem.getFirst().getPerformancePrice())
                    .build();

            String formatPrice = formatPrice(order.getTotalPrice());
            mailSendService.joinPaymentEmail(order.getUser().getEmail(), formatPrice);

            return orderInfo;
        } catch (Exception e) {
            throw e;
        }
    }

    @Transactional
    public String handleNicepayReturn(Map<String, String> params) {
        log.info("NICEPAY RETURN PARAMS = {}", params);

        try {
            String resultCode = params.get("authResultCode");
            String authToken = params.get("authToken");
            String tid = params.get("tid");
            String orderIdStr = params.get("orderId");
            String amount = params.get("amount") != null ? params.get("amt") : params.get("amount");

            if (orderIdStr == null) {
                throw new RuntimeException("orderId가 존재하지 않음");
            }

            Long orderId = Long.valueOf(orderIdStr);

            // 1) 인증 결과 실패
            if (!"0000".equals(resultCode)) {
                throw new RuntimeException("인증 실패");
            }

            // 2) NICEPAY 서버 승인 API 호출
            NicepayApproveResponseDTO approveResult = callApprove(authToken, amount);

            if (approveResult == null || !"0000".equals(approveResult.getResultCode())) {
                throw new RuntimeException("승인 API 실패");
            }

            // 3) DB 주문 상태 업데이트
            OrdersEntity order = orderRepo.findById(orderId).orElseThrow(() -> new RuntimeException("Order not found"));

            order.setTid(tid);
            order.setOrderStatus(OrderStatus.PASS);

            log.info("결제 완료 처리됨: orderId={}, tid={}", orderId, tid);

            // 4) 성공 redirect URL 반환
            return "https://www.podo-store.com/purchase/success?orderId=" + orderId;
        } catch (Exception e) {
            throw e;
        }
    }

    public NicepayApproveResponseDTO callApprove(String tid, String amount) {
        RestTemplate restTemplate = new RestTemplate();
        String url = "https://api.nicepay.co.kr/v1/payments/approve";

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("clientId", clientId);
        body.add("secretKey", secretKey);
        body.add("tid", tid);
        body.add("amount", amount);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<NicepayApproveResponseDTO> res = restTemplate.postForEntity(url, entity, NicepayApproveResponseDTO.class);

            return res.getBody();
        } catch (Exception e) {
            log.error("승인 API 호출 실패", e);
            return null;
        }
    }

    // =============== private (protected) method ===============
    private ProductEntity getProduct(UUID id) {
        try {
            return productRepo.findById(id);
        } catch (Exception e) {
            throw new RuntimeException("상품 조회 실패", e);
        }
    }

    private boolean buyPerformance(final Long id) {
        try {
            List<OrderItemEntity> orderItems = orderItemRepo.findByOrderId(id);

            for(OrderItemEntity orderItem : orderItems) {
                if(orderItem.getPerformanceAmount() > 0)
                    return true;
            }

            return false;
        } catch (Exception e) {
            throw new RuntimeException("공연권 구매 확인 실패", e);
        }
    }

    @Transactional
    protected OrdersEntity orderCreate(final OrdersEntity ordersEntity, final OrderRequestDTO orderRequestDTO, final UserEntity user) {
        try {
            // dto로 받은 주문 목록에서 item을 하나씩 뽑아서 가공
            final List<OrderItemEntity> orderItems = orderRequestDTO.getOrderItem().stream().map(orderItemDTO -> {
                final OrderItemEntity orderItem = new OrderItemEntity();
                orderItem.setOrder(ordersEntity);

                final ProductEntity product = productRepo.findById(orderItemDTO.getProductId());

                if(product == null)
                    throw new RuntimeException("물건이 존재하지 않음");

                if(user.getId().equals(product.getUser().getId()))
                    throw new RuntimeException("본인 작품 구매 불가");

                // 대본권 구매 확인
                if (!product.getScript() && orderItemDTO.getScript())
                    throw new RuntimeException("대본권 구매 조건 위반");

                // 공연권 구매 확인
                if (!product.getPerformance() && (orderItemDTO.getPerformanceAmount() > 0))
                    throw new RuntimeException("공연권 구매 조건 위반");

                if(orderItemRepo.existsByProductIdAndUserId(orderItemDTO.getProductId(), user.getId())) {
                    final List<OrderItemEntity> items = orderItemRepo.findByProductIdAndUserId(orderItemDTO.getProductId(), user.getId());

                    for(OrderItemEntity item : items) {
                        // 대본권 제한
                        if(orderItemDTO.getScript() && item.getScript())
                            throw new RuntimeException("<" + product.getTitle() + "> 대본은 이미 구매했음");
                    }
                } else {
                    if(!orderItemDTO.getScript() && orderItemDTO.getPerformanceAmount() > 0)
                        throw new RuntimeException("대본권을 구매해야 함");
                }

                final long scriptPrice = orderItemDTO.getScript() ? product.getScriptPrice() : 0;
                final long performancePrice = orderItemDTO.getPerformanceAmount() > 0 ? product.getPerformancePrice() * orderItemDTO.getPerformanceAmount() : 0;
                final long totalPrice = scriptPrice + performancePrice;

                orderItem.setProduct(product);
                orderItem.setScript(orderItemDTO.getScript());
                orderItem.setScriptPrice(scriptPrice);
                orderItem.setPerformanceAmount(orderItemDTO.getPerformanceAmount());
                orderItem.setPerformancePrice(performancePrice);
                orderItem.setTotalPrice(totalPrice);
                orderItem.setWriteId(product.getUser().getId());
                orderItem.setUser(user);

                return orderItem;
            }).toList();

            ordersEntity.setOrderItem(orderItems);
            ordersEntity.setTotalPrice(orderItems.stream().mapToLong(OrderItemEntity::getTotalPrice).sum());

            return orderRepo.save(ordersEntity);
        } catch (Exception e) {
            throw new RuntimeException("주문 생성 실패", e);
        }
    }

    @Transactional
    protected void createApplicant(final ApplicantEntity applicant) {
        try {
            final String number_regex = "^01(?:0|1|[6-9])-(?:\\d{3}|\\d{4})-\\d{4}$";

            if(applicant.getName().isBlank())
                throw new RuntimeException("이름에 공백 불가");

            if(applicant.getPhoneNumber().isBlank() || !applicant.getPhoneNumber().matches(number_regex))
                throw new RuntimeException("전화번호가 올바르지 않음");

            if(applicant.getAddress().isBlank())
                throw new RuntimeException("주소가 올바르지 않음");

            applicantRepo.save(applicant);
        } catch (Exception e) {
            throw new RuntimeException("신청자 정보 생성 실패", e);
        }
    }

    private List<OrderCompleteResponseDTO> orderResult(final OrdersEntity ordersEntity) {
        try {
            List<OrderItemEntity> orderItems = orderItemRepo.findByOrderId(ordersEntity.getId());

            return orderItems.stream().map(orderItem ->
                    OrderCompleteResponseDTO.builder()
                            .id(ordersEntity.getId())
                            .orderDate(ordersEntity.getCreatedAt())
                            .orderNum(ordersEntity.getId())
                            .scriptPrice(orderItem.getScriptPrice())
                            .performancePrice(orderItem.getPerformancePrice())
                            .totalPrice(orderItem.getTotalPrice())
                            .build()
            ).toList();
        } catch (Exception e) {
            throw new RuntimeException("주문 결과 조회 실패", e);
        }
    }

    private List<OrderItemEntity> getOrderItem(final Long orderId) {
        try {
            return orderItemRepo.findByOrderId(orderId);
        } catch (Exception e) {
            throw new RuntimeException("주문 항목 조회 실패", e);
        }
    }

    private OrdersEntity getOrderInfo(final Long orderId) {
        try {
            return orderRepo.findById(orderId).orElse(null);
        } catch (Exception e) {
            throw new RuntimeException("주문 정보 조회 실패", e);
        }
    }

    private String formatPrice(long totalPrice) {
        DecimalFormat formatter = new DecimalFormat("#,###");
        return formatter.format(totalPrice);
    }
}
