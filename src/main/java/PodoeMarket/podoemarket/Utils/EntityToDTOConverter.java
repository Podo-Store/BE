package PodoeMarket.podoemarket.Utils;

import PodoeMarket.podoemarket.common.entity.ApplicantEntity;
import PodoeMarket.podoemarket.common.entity.OrderItemEntity;
import PodoeMarket.podoemarket.common.entity.OrdersEntity;
import PodoeMarket.podoemarket.common.entity.ProductEntity;
import PodoeMarket.podoemarket.dto.*;
import PodoeMarket.podoemarket.dto.response.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class EntityToDTOConverter {
    public static OrderScriptDTO convertToScriptOrderItemDTO(OrderItemEntity orderItem, ProductEntity product, String bucketURL) {
        try {
            OrderScriptDTO itemDTO = new OrderScriptDTO();

            itemDTO.setId(orderItem.getId());
            itemDTO.setTitle(orderItem.getTitle());
            itemDTO.setScript(orderItem.getScript());

            if(product != null) { // 삭제된 작품이 아닐 경우
                String encodedScriptImage = product.getImagePath() != null ? bucketURL + URLEncoder.encode(product.getImagePath(), "UTF-8") : "";

                itemDTO.setDelete(false);
                itemDTO.setWriter(product.getWriter());
                itemDTO.setImagePath(encodedScriptImage);
                itemDTO.setChecked(product.getChecked());
                itemDTO.setScriptPrice(orderItem.getScript() ? product.getScriptPrice() : 0);
                itemDTO.setProductId(product.getId());
                itemDTO.setOrderStatus(orderItem.getOrder().getOrderStatus());
            } else { // 삭제된 작품일 경우
                itemDTO.setDelete(true);
                itemDTO.setScriptPrice(orderItem.getScript() ? orderItem.getScriptPrice() : 0);
            }

            return itemDTO;
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public static OrderPerformanceDTO convertToPerformanceOrderItemDTO(OrderItemEntity orderItem, ProductEntity product, String bucketURL, int dateCount) {
        try {
            OrderPerformanceDTO itemDTO = new OrderPerformanceDTO();

            itemDTO.setId(orderItem.getId());
            itemDTO.setTitle(orderItem.getTitle());
            itemDTO.setPerformanceAmount(orderItem.getPerformanceAmount());

            if(LocalDateTime.now().isAfter(orderItem.getCreatedAt().plusYears(1)))
                itemDTO.setPossibleCount(0);
            else
                itemDTO.setPossibleCount(orderItem.getPerformanceAmount() - dateCount);

            if(product != null) { // 삭제된 작품이 아닐 경우
                String encodedScriptImage = product.getImagePath() != null ? bucketURL + URLEncoder.encode(product.getImagePath(), "UTF-8") : "";

                itemDTO.setDelete(false);
                itemDTO.setWriter(product.getWriter());
                itemDTO.setImagePath(encodedScriptImage);
                itemDTO.setChecked(product.getChecked());
                itemDTO.setPerformancePrice(orderItem.getPerformanceAmount() > 0 ? product.getPerformancePrice() : 0);
                itemDTO.setPerformanceTotalPrice(orderItem.getPerformancePrice());
                itemDTO.setProductId(product.getId());
                itemDTO.setOrderStatus(orderItem.getOrder().getOrderStatus());
            } else { // 삭제된 작품일 경우
                itemDTO.setDelete(true);
                itemDTO.setPerformancePrice(orderItem.getPerformanceAmount() > 0 ? product.getPerformancePrice() : 0);
                itemDTO.setPerformanceTotalPrice(orderItem.getPerformancePrice());
            }

            return itemDTO;
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public static OrderCompleteDTO convertToOrderCompleteDTO(OrdersEntity ordersEntity, OrderItemEntity orderItem) {
        OrderCompleteDTO completeDTO = new OrderCompleteDTO();

        completeDTO.setId(ordersEntity.getId());
        completeDTO.setOrderDate(ordersEntity.getCreatedAt());
        completeDTO.setOrderNum(ordersEntity.getId());
        completeDTO.setTitle(orderItem.getTitle());
        completeDTO.setScriptPrice(orderItem.getScriptPrice());
        completeDTO.setPerformancePrice(orderItem.getPerformancePrice());
        completeDTO.setPerformanceAmount(orderItem.getPerformanceAmount());
        completeDTO.setTotalPrice(ordersEntity.getTotalPrice());

        return completeDTO;
    }

    public static ApplyDTO convertToApplyDTO(OrderItemEntity orderItemEntity, ApplicantEntity applicantEntity, String bucketURL) throws UnsupportedEncodingException {
      ApplyDTO applyDTO = new ApplyDTO();

      applyDTO.setOrderItemId(orderItemEntity.getId());
      applyDTO.setImagePath(orderItemEntity.getProduct().getImagePath() != null ? bucketURL + URLEncoder.encode(orderItemEntity.getProduct().getImagePath(), "UTF-8"): "");
      applyDTO.setTitle(orderItemEntity.getTitle());
      applyDTO.setWriter(orderItemEntity.getProduct().getWriter());
      applyDTO.setPerformanceAmount(orderItemEntity.getPerformanceAmount());

       ApplicantDTO applicantDTO = ApplicantDTO.builder()
               .name(applicantEntity.getName())
               .phoneNumber(applicantEntity.getPhoneNumber())
               .address(applicantEntity.getAddress())
               .build();

       applyDTO.setApplicant(applicantDTO);

       List<PerformanceDateDTO> performanceDateDTO = orderItemEntity.getPerformanceDate()
               .stream()
               .map(performanceDate -> new PerformanceDateDTO(performanceDate.getDate()))
               .toList();

       applyDTO.setPerformanceDate(performanceDateDTO);

      return applyDTO;
    }
}
