package PodoeMarket.podoemarket.Utils;

import PodoeMarket.podoemarket.dto.*;
import PodoeMarket.podoemarket.dto.response.*;
import PodoeMarket.podoemarket.entity.*;
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

    public static ProductListDTO convertToProductList(ProductEntity entity, String bucketURL) {
       try {
           ProductListDTO productListDTO = new ProductListDTO();
           String encodedScriptImage = entity.getImagePath() != null ? bucketURL + URLEncoder.encode(entity.getImagePath(), "UTF-8") : "";

           productListDTO.setId(entity.getId());
           productListDTO.setTitle(entity.getTitle());
           productListDTO.setWriter(entity.getWriter());
           productListDTO.setImagePath(encodedScriptImage);
           productListDTO.setScript(entity.isScript());
           productListDTO.setScriptPrice(entity.getScriptPrice());
           productListDTO.setPerformance(entity.isPerformance());
           productListDTO.setPerformancePrice(entity.getPerformancePrice());
           productListDTO.setDate(entity.getCreatedAt());
           productListDTO.setChecked(entity.isChecked());

           return productListDTO;
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public static ProductDTO convertToSingleProductDTO(ProductEntity entity, boolean isBuyScript, String bucketURL) {
        try {
            ProductDTO productDTO = new ProductDTO();
            String encodedScriptImage = entity.getImagePath() != null ? bucketURL + URLEncoder.encode(entity.getImagePath(), "UTF-8") : "";
            String encodedDescription = entity.getDescriptionPath() != null ? bucketURL + URLEncoder.encode(entity.getDescriptionPath(), "UTF-8") : "";

            productDTO.setId(entity.getId());
            productDTO.setTitle(entity.getTitle());
            productDTO.setWriter(entity.getWriter());
            productDTO.setFilePath(entity.getFilePath());
            productDTO.setImagePath(encodedScriptImage);
            productDTO.setScript(entity.isScript());
            productDTO.setScriptPrice(entity.getScriptPrice());
            productDTO.setPerformance(entity.isPerformance());
            productDTO.setPerformancePrice(entity.getPerformancePrice());
            productDTO.setDescriptionPath(encodedDescription);
            productDTO.setDate(entity.getCreatedAt());
            productDTO.setChecked(entity.isChecked());
            productDTO.setPlayType(entity.getPlayType());

            productDTO.setBuyScript(isBuyScript);

            return productDTO;
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public static OrderScriptDTO convertToScriptOrderItemDTO(OrderItemEntity orderItem, ProductEntity product, String bucketURL) {
        try {
            OrderScriptDTO itemDTO = new OrderScriptDTO();

            itemDTO.setId(orderItem.getId());
            itemDTO.setTitle(orderItem.getTitle());
            itemDTO.setScript(orderItem.isScript());

            if(product != null) { // 삭제된 작품이 아닐 경우
                String encodedScriptImage = product.getImagePath() != null ? bucketURL + URLEncoder.encode(product.getImagePath(), "UTF-8") : "";

                itemDTO.setDelete(false);
                itemDTO.setWriter(product.getWriter());
                itemDTO.setImagePath(encodedScriptImage);
                itemDTO.setChecked(product.isChecked());
                itemDTO.setScriptPrice(orderItem.isScript() ? product.getScriptPrice() : 0);
                itemDTO.setProductId(product.getId());
            } else { // 삭제된 작품일 경우
                itemDTO.setDelete(true);
                itemDTO.setScriptPrice(orderItem.isScript() ? orderItem.getScriptPrice() : 0);
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
                itemDTO.setChecked(product.isChecked());
                itemDTO.setPerformancePrice(orderItem.getPerformanceAmount() > 0 ? product.getPerformancePrice() : 0);
                itemDTO.setPerformanceTotalPrice(orderItem.getPerformancePrice());
                itemDTO.setProductId(product.getId());
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

        completeDTO.setOrderDate(ordersEntity.getCreatedAt());
        completeDTO.setOrderNum(ordersEntity.getId());
        completeDTO.setTitle(orderItem.getTitle());
        completeDTO.setScriptPrice(orderItem.getScriptPrice());
        completeDTO.setPerformancePrice(orderItem.getPerformancePrice());
        completeDTO.setPerformanceAmount(orderItem.getPerformanceAmount());
        completeDTO.setTotalPrice(ordersEntity.getTotalPrice());

        return completeDTO;
    }

    public static ApplyDTO convertToApplyDTO(OrderItemEntity orderItemEntity, ApplicantEntity applicantEntity) {
      ApplyDTO applyDTO = new ApplyDTO();

      applyDTO.setOrderItemId(orderItemEntity.getId());
      applyDTO.setImagePath(orderItemEntity.getProduct().getImagePath());
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
