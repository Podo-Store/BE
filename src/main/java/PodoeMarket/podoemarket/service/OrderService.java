package PodoeMarket.podoemarket.service;

import PodoeMarket.podoemarket.dto.OrderDTO;
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
import java.util.UUID;

@RequiredArgsConstructor
@Slf4j
@Service
public class OrderService {
    private final ProductRepository productRepo;
    private final OrderRepository orderRepo;
    private final OrderItemRepository orderItemRepo;

    public void orderCreate(final OrdersEntity ordersEntity, final OrderDTO orderDTO, final UserEntity user) {
        // dto로 받은 주문 목록에서 item을 하나씩 뽑아서 가공
        List<OrderItemEntity> orderItems = orderDTO.getOrderItem().stream().map(OrderItemDTO -> {
            OrderItemEntity orderItem = new OrderItemEntity();
            orderItem.setOrder(ordersEntity);

            ProductEntity product = productRepo.findById(OrderItemDTO.getProductId());

            if(product == null) {
                throw new RuntimeException("물건이 존재하지 않음");
            }

            if(orderItemRepo.existsByProductIdAndUserId(OrderItemDTO.getProductId(), user.getId())) {
                OrderItemEntity item = orderItemRepo.findByProductIdAndUserId(OrderItemDTO.getProductId(), user.getId());

                if(item.isScript()) {
                    throw new RuntimeException("<" + product.getTitle() + "> 이미 구매했음");
                }
            }

            int totalPrice = OrderItemDTO.getScriptPrice() + OrderItemDTO.getPerformancePrice();

            orderItem.setProduct(product);
            orderItem.setScript(OrderItemDTO.isScript());
            orderItem.setScriptPrice(OrderItemDTO.getScriptPrice());
            orderItem.setPerformance(OrderItemDTO.isPerformance());

            if(OrderItemDTO.isPerformance()) {
                orderItem.setContractStatus(1);
            }

            orderItem.setPerformancePrice(OrderItemDTO.getPerformancePrice());
            orderItem.setTotalPrice(totalPrice);
            orderItem.setUser(user);

            return orderItem;
        }).toList();

        ordersEntity.setOrderItem(orderItems);
        ordersEntity.setTotalPrice(orderItems.stream().mapToInt(OrderItemEntity::getTotalPrice).sum());
        orderRepo.save(ordersEntity);
    }
}
