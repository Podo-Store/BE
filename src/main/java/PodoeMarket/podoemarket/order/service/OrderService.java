package PodoeMarket.podoemarket.order.service;

import PodoeMarket.podoemarket.common.entity.*;
import PodoeMarket.podoemarket.common.entity.type.OrderStatus;
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
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.List;
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

    public OrderItemResponseDTO getOrderItemInfo(UserEntity userInfo, OrderItemResponseDTO dto) {
        try {
            final ProductEntity orderProduct = getProduct(dto.getProductId());

            if(orderProduct == null)
                throw new RuntimeException("작품 정보 조회 실패");

            if(userInfo.getId().equals(orderProduct.getUser().getId()))
                throw new RuntimeException("본인 작품 구매 불가");

            final int totalPrice = (dto.isScript() ? orderProduct.getScriptPrice() : 0) + (dto.getPerformanceAmount() > 0 ? orderProduct.getPerformancePrice() * dto.getPerformanceAmount() : 0);
            final String encodedScriptImage = orderProduct.getImagePath() != null ? bucketURL + URLEncoder.encode(orderProduct.getImagePath(), StandardCharsets.UTF_8) : "";

            return OrderItemResponseDTO.builder()
                    .title(orderProduct.getTitle())
                    .writer(orderProduct.getWriter())
                    .imagePath(encodedScriptImage)
                    .playType(orderProduct.getPlayType())
                    .script(dto.isScript())
                    .scriptPrice(dto.isScript() ? orderProduct.getScriptPrice() : 0)
                    .performanceAmount(dto.getPerformanceAmount())
                    .performancePrice(orderProduct.getPerformancePrice())
                    .performanceTotalPrice(dto.getPerformanceAmount() > 0 ? orderProduct.getPerformancePrice() * dto.getPerformanceAmount() : 0)
                    .totalPrice(totalPrice)
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("주문 상품 정보 조회 중 오류 발생", e);
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
            throw new RuntimeException("주문 처리 실패", e);
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
                    .title(orderItem.getFirst().getTitle())
                    .script(orderItem.getFirst().getScript())
                    .scriptPrice(orderItem.getFirst().getScriptPrice())
                    .performanceAmount(orderItem.getFirst().getPerformanceAmount())
                    .performancePrice(orderItem.getFirst().getPerformancePrice())
                    .build();

            String formatPrice = formatPrice(order.getTotalPrice());
            mailSendService.joinPaymentEmail(order.getUser().getEmail(), formatPrice);

            return orderInfo;
        } catch (Exception e) {
            throw new RuntimeException("주문 성공 처리 중 오류 발생", e);
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

                // 대본권, 공연권 1일 때만 구매 가능
                if ((!product.getScript() && orderItemDTO.isScript()) || (!product.getPerformance() && (orderItemDTO.getPerformanceAmount() > 0)))
                    throw new RuntimeException("구매 조건 확인");

                if(orderItemRepo.existsByProductIdAndUserId(orderItemDTO.getProductId(), user.getId())) {
                    final List<OrderItemEntity> items = orderItemRepo.findByProductIdAndUserId(orderItemDTO.getProductId(), user.getId());

                    for(OrderItemEntity item : items) {
                        // 대본권 제한
                        if(orderItemDTO.isScript() && item.getScript())
                            throw new RuntimeException("<" + product.getTitle() + "> 대본은 이미 구매했음");
                    }
                } else {
                    if(!orderItemDTO.isScript() && orderItemDTO.getPerformanceAmount() > 0)
                        throw new RuntimeException("대본권을 구매해야 함");
                }

                final int scriptPrice = orderItemDTO.isScript() ? product.getScriptPrice() : 0;
                final int performancePrice = orderItemDTO.getPerformanceAmount() > 0 ? product.getPerformancePrice() * orderItemDTO.getPerformanceAmount() : 0;
                final int totalPrice = scriptPrice + performancePrice;

                orderItem.setProduct(product);
                orderItem.setScript(orderItemDTO.isScript());
                orderItem.setScriptPrice(scriptPrice);
                orderItem.setPerformanceAmount(orderItemDTO.getPerformanceAmount());
                orderItem.setPerformancePrice(performancePrice);
                orderItem.setTotalPrice(totalPrice);
                orderItem.setTitle(product.getTitle());
                orderItem.setUser(user);

                return orderItem;
            }).toList();

            ordersEntity.setOrderItem(orderItems);
            ordersEntity.setTotalPrice(orderItems.stream().mapToInt(OrderItemEntity::getTotalPrice).sum());

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
                            .title(orderItem.getTitle())
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

    private String formatPrice(int totalPrice) {
        DecimalFormat formatter = new DecimalFormat("#,###");
        return formatter.format(totalPrice);
    }
}
