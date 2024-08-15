package PodoeMarket.podoemarket.service;

import PodoeMarket.podoemarket.dto.OrderDTO;
import PodoeMarket.podoemarket.dto.OrderItemDTO;
import PodoeMarket.podoemarket.entity.OrderItemEntity;
import PodoeMarket.podoemarket.entity.OrdersEntity;
import PodoeMarket.podoemarket.entity.ProductEntity;
import PodoeMarket.podoemarket.entity.UserEntity;
import PodoeMarket.podoemarket.repository.OrderItemRepository;
import PodoeMarket.podoemarket.repository.OrderRepository;
import PodoeMarket.podoemarket.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Slf4j
@Service
public class OrderService {
    private final ProductRepository productRepo;
    private final OrderRepository orderRepo;
    private final OrderItemRepository orderItemRepo;

    public void orderCreate(final OrdersEntity ordersEntity, final OrderDTO orderDTO, final UserEntity user) {
        // dto로 받은 주문 목록에서 item을 하나씩 뽑아서 가공
        final List<OrderItemEntity> orderItems = orderDTO.getOrderItem().stream().map(orderItemDTO -> {
            final OrderItemEntity orderItem = new OrderItemEntity();
            orderItem.setOrder(ordersEntity);

            final ProductEntity product = productRepo.findById(orderItemDTO.getProductId());

            if(product == null) {
                throw new RuntimeException("물건이 존재하지 않음");
            }

            if(orderItemRepo.existsByProductIdAndUserId(orderItemDTO.getProductId(), user.getId())) {
                final List<OrderItemEntity> items = orderItemRepo.findByProductIdAndUserId(orderItemDTO.getProductId(), user.getId());

                for(OrderItemEntity item : items) {
                    // 대본권 제한
                    if(orderItemDTO.isScript() && item.isScript()) {
                        throw new RuntimeException("<" + product.getTitle() + "> 대본은 이미 구매했음");
                    }

                    // 공연권 제한
                    if(orderItemDTO.isPerformance() && item.getContractStatus() != 3) {
                        throw new RuntimeException("<" + product.getTitle() + "> 공연권은 이미 구매했음");
                    }
                }
            } else {
                if(!orderItemDTO.isScript() && orderItemDTO.isPerformance()) {
                    throw new RuntimeException("대본권을 구매해야 함");
                }
            }

            final int totalPrice = orderItemDTO.getScriptPrice() + orderItemDTO.getPerformancePrice();

            orderItem.setProduct(product);
            orderItem.setScript(orderItemDTO.isScript());
            orderItem.setScriptPrice(orderItemDTO.getScriptPrice());
            orderItem.setPerformance(orderItemDTO.isPerformance());

            if(orderItemDTO.isPerformance()) {
                orderItem.setContractStatus(1);
            }

            orderItem.setPerformancePrice(orderItemDTO.getPerformancePrice());
            orderItem.setTotalPrice(totalPrice);
            orderItem.setUser(user);

            return orderItem;
        }).toList();

        ordersEntity.setOrderItem(orderItems);
        ordersEntity.setTotalPrice(orderItems.stream().mapToInt(OrderItemEntity::getTotalPrice).sum());
        orderRepo.save(ordersEntity);
    }
}
