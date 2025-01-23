package PodoeMarket.podoemarket.service;

import PodoeMarket.podoemarket.Utils.EntityToDTOConverter;
import PodoeMarket.podoemarket.common.entity.*;
import PodoeMarket.podoemarket.dto.response.OrderCompleteDTO;
import PodoeMarket.podoemarket.dto.OrderDTO;
import PodoeMarket.podoemarket.common.repository.ApplicantRepository;
import PodoeMarket.podoemarket.common.repository.OrderItemRepository;
import PodoeMarket.podoemarket.common.repository.OrderRepository;
import PodoeMarket.podoemarket.common.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.text.DecimalFormat;
import java.util.List;

@RequiredArgsConstructor
@Slf4j
@Service
public class OrderService {
    private final ProductRepository productRepo;
    private final OrderRepository orderRepo;
    private final OrderItemRepository orderItemRepo;
    private final ApplicantRepository applicantRepo;

    public OrdersEntity orderCreate(final OrdersEntity ordersEntity, final OrderDTO orderDTO, final UserEntity user) {
        // dto로 받은 주문 목록에서 item을 하나씩 뽑아서 가공
        final List<OrderItemEntity> orderItems = orderDTO.getOrderItem().stream().map(orderItemDTO -> {
            final OrderItemEntity orderItem = new OrderItemEntity();
            orderItem.setOrder(ordersEntity);

            final ProductEntity product = productRepo.findById(orderItemDTO.getProductId());

            if(product == null)
                throw new RuntimeException("물건이 존재하지 않음");

            if(user.getId().equals(product.getUser().getId()))
                throw new RuntimeException("본인 작품 구매 불가");

            // 대본권, 공연권 1일 때만 구매 가능
            if ((!product.isScript() && orderItemDTO.isScript()) || (!product.isPerformance() && (orderItemDTO.getPerformanceAmount() > 0)))
                throw new RuntimeException("구매 조건 확인");

            if(orderItemRepo.existsByProductIdAndUserId(orderItemDTO.getProductId(), user.getId())) {
                final List<OrderItemEntity> items = orderItemRepo.findByProductIdAndUserId(orderItemDTO.getProductId(), user.getId());

                for(OrderItemEntity item : items) {
                    // 대본권 제한
                    if(orderItemDTO.isScript() && item.isScript())
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
    }

    public List<OrderCompleteDTO> orderResult(final OrdersEntity ordersEntity) {
        List<OrderItemEntity> orderItems = orderItemRepo.findByOrderId(ordersEntity.getId());

        return orderItems.stream().map(orderItem -> EntityToDTOConverter.convertToOrderCompleteDTO(ordersEntity, orderItem)).toList();
    }

    public boolean buyPerformance(final Long id) {
        List<OrderItemEntity> orderItems = orderItemRepo.findByOrderId(id);

        for(OrderItemEntity orderItem : orderItems) {
            if(orderItem.getPerformanceAmount() > 0)
                return true;
        }

        return false;
    }

    public void createApplicant(final ApplicantEntity applicant) {
        final String number_regex = "^01(?:0|1|[6-9])-(?:\\d{3}|\\d{4})-\\d{4}$";

        if(applicant.getName().isBlank())
            throw new RuntimeException("이름에 공백 불가");

        if(applicant.getPhoneNumber().isBlank() || !applicant.getPhoneNumber().matches(number_regex))
            throw new RuntimeException("전화번호가 올바르지 않음");

        if(applicant.getAddress().isBlank())
            throw new RuntimeException("주소가 올바르지 않음");

        applicantRepo.save(applicant);
    }

    public List<OrderItemEntity> getOrderItem(final Long orderId) {
        return orderItemRepo.findByOrderId(orderId);
    }

    public OrdersEntity getOrderInfo(final Long orderId) {
        return orderRepo.findById(orderId).orElse(null);
    }

    public String formatPrice(int totalPrice) {
        DecimalFormat formatter = new DecimalFormat("#,###");
        return formatter.format(totalPrice);
    }
}
