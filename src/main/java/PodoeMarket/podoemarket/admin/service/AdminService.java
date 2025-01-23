package PodoeMarket.podoemarket.admin.service;

import PodoeMarket.podoemarket.admin.dto.response.OrderManagementResponseDTO;
import PodoeMarket.podoemarket.admin.dto.response.ProductManagementResponseDTO;
import PodoeMarket.podoemarket.common.entity.OrderItemEntity;
import PodoeMarket.podoemarket.common.entity.OrdersEntity;
import PodoeMarket.podoemarket.common.entity.ProductEntity;
import PodoeMarket.podoemarket.common.entity.UserEntity;
import PodoeMarket.podoemarket.common.entity.type.ProductStatus;
import PodoeMarket.podoemarket.common.repository.OrderItemRepository;
import PodoeMarket.podoemarket.common.repository.OrderRepository;
import PodoeMarket.podoemarket.common.repository.ProductRepository;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.S3Object;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Slf4j
@Service
public class AdminService {
    private final ProductRepository productRepo;
    private final OrderRepository orderRepo;
    private final OrderItemRepository orderItemRepo;
    private final AmazonS3 amazonS3;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    public void checkAuth(final UserEntity user) {
        if (!user.isAuth())
            throw new RuntimeException("어드민이 아닙니다.");
    }

    public Long getCheckedCount(final ProductStatus productStatus) {
        return productRepo.countAllByChecked(productStatus);
    }

    public Page<ProductEntity> getAllProducts(final String search, final ProductStatus status, final int page) {
        final PageRequest pageRequest = PageRequest.of(page, 10, Sort.by("createdAt").descending());

        if (search == null || search.trim().isEmpty()) {
            if (status == null) // 검색어 X, 전체 O
                return productRepo.findAll(pageRequest);
            else // 검색어 X, 전체 X
                return productRepo.findByChecked(status, pageRequest);
        } else {
            if (status == null) // 검색어 O, 전체 O
                return productRepo.findByTitleContainingOrWriterContaining(search, search, pageRequest);
            else // 검색어 O, 전체 X
                return productRepo.findByTitleContainingOrWriterContainingAndChecked(search, search, status, pageRequest);
        }
    }

    public List<ProductManagementResponseDTO.ProductDTO> getProductList(Page<ProductEntity> productsPage) {
        return productsPage.getContent().stream()
                .map(product -> ProductManagementResponseDTO.ProductDTO.builder()
                        .id(product.getId())
                        .createdAt(product.getCreatedAt())
                        .title(product.getTitle())
                        .writer(product.getWriter())
                        .checked(product.getChecked())
                        .playType(product.getPlayType())
                        .build())
                .collect(Collectors.toList());
    }

    public ProductEntity getProduct(final UUID id) {
        return productRepo.findById(id);
    }

    public void updateProduct(final ProductEntity product) {
        productRepo.save(product);
    }

    public void checkExpire(final LocalDateTime updatedAt, final ProductStatus productStatus) {
        if (productStatus == ProductStatus.REJECT && updatedAt.isAfter(LocalDateTime.now().plusDays(7))) {
            throw new RuntimeException("등록 거절 이후 7일이 지났습니다.");
        }
    }

    public byte[] downloadFile(final String fileKey) {
        try (S3Object s3Object = amazonS3.getObject(bucket, fileKey);
             InputStream inputStream = s3Object.getObjectContent();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            // 버퍼를 사용하여 데이터 읽기
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            return outputStream.toByteArray();
        } catch (AmazonS3Exception e) {
            throw new RuntimeException("S3에서 파일을 찾을 수 없습니다: " + fileKey);
        } catch (IOException e) {
            throw new RuntimeException("파일 다운로드 중 오류가 발생했습니다: " + fileKey);
        }
    }

    public Long getOrderStatusCount(final Boolean paymentStatus) {
        return orderRepo.countAllByPaymentStatus(paymentStatus);
    }

    // 검색어가 없을 경우
    @Transactional
    public OrderManagementResponseDTO getAllOrders(final Boolean checked, final int page) {
        final PageRequest pageRequest = PageRequest.of(page, 10, Sort.by("createdAt").descending());
        final Page<OrdersEntity> orders;

        if (checked == null) // 검색어 X, 전체 O
            orders = orderRepo.findAll(pageRequest);
        else // 검색어 X, 전체 X
            orders = orderRepo.findAllByPaymentStatus(checked, pageRequest);

        List<OrderManagementResponseDTO.OrderDTO> orderList = orders.getContent().stream()
                .map(order -> OrderManagementResponseDTO.OrderDTO.builder()
                        .id(order.getId())
                        .orderDate(order.getCreatedAt())
                        .title(order.getOrderItem().getFirst().getTitle())
                        .writer(order.getOrderItem().getFirst().getProduct().getWriter())
                        .customer(order.getOrderItem().getFirst().getUser().getNickname())
                        .paymentStatus(order.isPaymentStatus())
                        .script(order.getOrderItem().getFirst().isScript())
                        .performanceAmount(order.getOrderItem().getFirst().getPerformanceAmount())
                        .totalPrice(order.getTotalPrice())
                        .build())
                .toList();

        return OrderManagementResponseDTO.builder()
                .orderCnt(orders.getTotalElements())
                .orders(orderList)
                .build();
    }

    // 검색어가 있는 경우
    @Transactional
    public OrderManagementResponseDTO getAllOrderItems(final String search, final Boolean checked, final int page) {
        final PageRequest pageRequest = PageRequest.of(page, 10, Sort.by("createdAt").descending());
        final Page<OrderItemEntity> orders;

        if (checked == null) // 검색어 O, 전체 O
            orders = orderItemRepo.findOrderItemsByKeyword(search, pageRequest);
        else // 검색어 O, 전체 X
            orders = orderItemRepo.findOrderItemsByKeywordAndPaymentStatus(search, checked, pageRequest);

        List<OrderManagementResponseDTO.OrderDTO> orderList = orders.getContent().stream()
                .map(orderItem -> OrderManagementResponseDTO.OrderDTO.builder()
                        .id(orderItem.getOrder().getId())
                        .orderDate(orderItem.getCreatedAt())
                        .title(orderItem.getTitle())
                        .writer(orderItem.getProduct().getWriter())
                        .customer(orderItem.getUser().getNickname())
                        .paymentStatus(orderItem.getOrder().isPaymentStatus())
                        .script(orderItem.isScript())
                        .performanceAmount(orderItem.getPerformanceAmount())
                        .totalPrice(orderItem.getTotalPrice())
                        .build())
                .toList();

        return OrderManagementResponseDTO.builder()
                .orderCnt(orders.getTotalElements())
                .orders(orderList)
                .build();
    }

    public OrdersEntity orders(final Long orderId) {
        return orderRepo.findById(orderId).orElse(null);
    }

    public void updateOrder(final OrdersEntity order) {
        orderRepo.save(order);
    }
}
