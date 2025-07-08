package PodoeMarket.podoemarket.admin.service;

import PodoeMarket.podoemarket.admin.dto.request.PlayTypeRequestDTO;
import PodoeMarket.podoemarket.admin.dto.response.OrderManagementResponseDTO;
import PodoeMarket.podoemarket.admin.dto.response.ProductManagementResponseDTO;
import PodoeMarket.podoemarket.common.entity.OrderItemEntity;
import PodoeMarket.podoemarket.common.entity.OrdersEntity;
import PodoeMarket.podoemarket.common.entity.ProductEntity;
import PodoeMarket.podoemarket.common.entity.UserEntity;
import PodoeMarket.podoemarket.common.entity.type.OrderStatus;
import PodoeMarket.podoemarket.common.entity.type.ProductStatus;
import PodoeMarket.podoemarket.common.entity.type.StageType;
import PodoeMarket.podoemarket.common.repository.OrderItemRepository;
import PodoeMarket.podoemarket.common.repository.OrderRepository;
import PodoeMarket.podoemarket.common.repository.ProductRepository;
import PodoeMarket.podoemarket.service.MailSendService;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.S3Object;
import org.springframework.transaction.annotation.Transactional;
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
@Transactional(readOnly = true)
public class AdminService {
    private final ProductRepository productRepo;
    private final OrderRepository orderRepo;
    private final OrderItemRepository orderItemRepo;
    private final AmazonS3 amazonS3;
    private final MailSendService mailSendService;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    public void checkAuth(final UserEntity user) {
        if (!user.isAuth())
            throw new RuntimeException("어드민이 아닙니다.");
    }

    public Long getCheckedCount(final ProductStatus productStatus) {
        try {
            return productRepo.countAllByChecked(productStatus);
        } catch (Exception e) {
            throw new RuntimeException("상품 상태 카운트 조회 실패", e);
        }
    }

    public Page<ProductEntity> getAllProducts(final String search, final ProductStatus status, final int page) {
        try {
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
        } catch (Exception e) {
            throw new RuntimeException("상품 목록 조회 실패", e);
        }
    }

    public List<ProductManagementResponseDTO.ProductDTO> getProductList(Page<ProductEntity> productsPage) {
        try {
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
        } catch (Exception e) {
            throw new RuntimeException("상품 DTO 변환 실패", e);
        }
    }

    public ProductEntity getProduct(final UUID id) {
        try {
            return productRepo.findById(id);
        } catch (Exception e) {
            throw new RuntimeException("상품 조회 실패", e);
        }
    }

    @Transactional
    public void updateProduct(final UUID productId, final PlayTypeRequestDTO dto) {
        try {
            ProductEntity product = getProduct(productId);

            if (dto.getPlayType() != null)
                product.setPlayType(dto.getPlayType());

            if (dto.getProductStatus() != null)
                product.setChecked(dto.getProductStatus());

            productRepo.save(product);

            if (dto.getProductStatus() == ProductStatus.PASS) {
                // 포도알 등급 부여
                if(product.getUser().getStageType() == null) {
                    product.getUser().setStageType(StageType.SINGLE_GRAPE);
                    productRepo.save(product);
                }

                mailSendService.joinRegisterPassMail(product.getUser().getEmail(), product.getTitle());
            }
            else if (dto.getProductStatus() == ProductStatus.REJECT)
                mailSendService.joinRegisterRejectMail(product.getUser().getEmail(), product.getTitle());

        } catch (Exception e) {
            throw new RuntimeException("상품 업데이트 실패", e);
        }
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

    public Long getOrderStatusCount(final OrderStatus orderStatus) {
        try {
            return orderRepo.countAllByOrderStatus(orderStatus);
        } catch (Exception e) {
            throw new RuntimeException("주문 상태 카운트 조회 실패", e);
        }
    }

    // 검색어가 없을 경우
    @Transactional
    public OrderManagementResponseDTO getAllOrders(final OrderStatus orderStatus, final int page) {
        try {
            final PageRequest pageRequest = PageRequest.of(page, 10, Sort.by("createdAt").descending());
            final Page<OrdersEntity> orders;

            if (orderStatus == null) // 검색어 X, 전체 O
                orders = orderRepo.findAll(pageRequest);
            else // 검색어 X, 전체 X
                orders = orderRepo.findAllByOrderStatus(orderStatus, pageRequest);

            List<OrderManagementResponseDTO.OrderDTO> orderList = orders.getContent().stream()
                    .map(order -> OrderManagementResponseDTO.OrderDTO.builder()
                            .id(order.getId())
                            .orderDate(order.getCreatedAt())
                            .title(order.getOrderItem().getFirst().getProduct().getTitle())
                            .writer(order.getOrderItem().getFirst().getProduct().getWriter())
                            .customer(order.getOrderItem().getFirst().getUser().getNickname())
                            .orderStatus(order.getOrderStatus())
                            .script(order.getOrderItem().getFirst().getScript())
                            .performanceAmount(order.getOrderItem().getFirst().getPerformanceAmount())
                            .totalPrice(order.getTotalPrice())
                            .build())
                    .toList();

            return OrderManagementResponseDTO.builder()
                    .orderCnt(orders.getTotalElements())
                    .orders(orderList)
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("주문 목록 조회 실패", e);
        }
    }

    // 검색어가 있는 경우
    @Transactional
    public OrderManagementResponseDTO getAllOrderItems(final String search, final OrderStatus orderStatus, final int page) {
        try {
            final PageRequest pageRequest = PageRequest.of(page, 10, Sort.by("createdAt").descending());
            final Page<OrderItemEntity> orders;

            if (orderStatus == null) // 검색어 O, 전체 O
                orders = orderItemRepo.findOrderItemsByKeyword(search, pageRequest);
            else // 검색어 O, 전체 X
                orders = orderItemRepo.findOrderItemsByKeywordAndOrderStatus(search, orderStatus, pageRequest);

            List<OrderManagementResponseDTO.OrderDTO> orderList = orders.getContent().stream()
                    .map(orderItem -> OrderManagementResponseDTO.OrderDTO.builder()
                            .id(orderItem.getOrder().getId())
                            .orderDate(orderItem.getCreatedAt())
                            .title(orderItem.getProduct().getTitle())
                            .writer(orderItem.getProduct().getWriter())
                            .customer(orderItem.getUser().getNickname())
                            .orderStatus(orderItem.getOrder().getOrderStatus())
                            .script(orderItem.getScript())
                            .performanceAmount(orderItem.getPerformanceAmount())
                            .totalPrice(orderItem.getTotalPrice())
                            .build())
                    .toList();

            return OrderManagementResponseDTO.builder()
                    .orderCnt(orders.getTotalElements())
                    .orders(orderList)
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("주문 항목 검색 실패", e);
        }
    }

    public OrdersEntity orders(final Long orderId) {
        try {
            return orderRepo.findOrderById(orderId);
        } catch (Exception e) {
            throw new RuntimeException("주문 조회 실패", e);
        }
    }

    @Transactional
    public void updateOrder(final OrdersEntity order) {
        try {
            orderRepo.save(order);
        } catch (Exception e) {
            throw new RuntimeException("주문 업데이트 실패", e);
        }
    }

    @Transactional
    public void updateTitle(UUID productId, String title) {
        try {
            ProductEntity product = productRepo.findById(productId);

            if (product == null)
                throw new RuntimeException("상품을 찾을 수 없습니다: " + productId);

            product.setTitle(title);
            productRepo.save(product);
        } catch (Exception e) {
            throw new RuntimeException("상품 제목 업데이트 실패", e);
        }
    }

    @Transactional
    public void updateWriter(UUID productId, String writer) {
        try {
            ProductEntity product = productRepo.findById(productId);

            if (product == null)
                throw new RuntimeException("상품을 찾을 수 없습니다: " + productId);

            product.setWriter(writer);
            productRepo.save(product);
        } catch (Exception e) {
            throw new RuntimeException("상품 제목 업데이트 실패", e);
        }
    }
}
