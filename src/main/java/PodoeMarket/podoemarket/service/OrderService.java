package PodoeMarket.podoemarket.service;

import PodoeMarket.podoemarket.dto.OrderDTO;
import PodoeMarket.podoemarket.entity.OrderItemEntity;
import PodoeMarket.podoemarket.entity.OrdersEntity;
import PodoeMarket.podoemarket.entity.ProductEntity;
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

    public void orderCreate(final OrdersEntity ordersEntity, final OrderDTO orderDTO) {
        // dto로 받은 주문 목록에서 item을 하나씩 뽑아서 가공
        List<OrderItemEntity> orderItems = orderDTO.getOrderItem().stream().map(OrderItemDTO -> {
            OrderItemEntity orderItem = new OrderItemEntity();
            orderItem.setOrder(ordersEntity);

            ProductEntity product = productRepo.findById(OrderItemDTO.getProductId());

            if (product == null) {
                throw new RuntimeException("물건이 존재하지 않음");
            }

            int totalPrice = OrderItemDTO.getScriptPrice() + OrderItemDTO.getPerformancePrice();

            orderItem.setProduct(product);
            orderItem.setScript(OrderItemDTO.isScript());
            orderItem.setScriptPrice(OrderItemDTO.getScriptPrice());
            orderItem.setPerformance(OrderItemDTO.isPerformance());
            orderItem.setPerformancePrice(OrderItemDTO.getPerformancePrice());
            orderItem.setTotalPrice(totalPrice);

            return orderItem;
        }).toList();

        ordersEntity.setOrderItem(orderItems);
        ordersEntity.setTotalPrice(orderItems.stream().mapToInt(OrderItemEntity::getTotalPrice).sum());
        orderRepo.save(ordersEntity);
    }
}
