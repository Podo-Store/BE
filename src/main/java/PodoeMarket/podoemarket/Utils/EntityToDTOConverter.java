package PodoeMarket.podoemarket.Utils;

import PodoeMarket.podoemarket.dto.*;
import PodoeMarket.podoemarket.entity.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
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
            String encodedFile = entity.getFilePath() != null ? bucketURL + URLEncoder.encode(entity.getFilePath(), "UTF-8") : "";
            String encodedDescription = entity.getDescriptionPath() != null ? bucketURL + URLEncoder.encode(entity.getDescriptionPath(), "UTF-8") : "";

            productDTO.setId(entity.getId());
            productDTO.setTitle(entity.getTitle());
            productDTO.setWriter(entity.getWriter());
            productDTO.setFilePath(encodedFile);
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

    public static OrderItemDTO convertToOrderItemDTO(OrderItemEntity orderItem, ProductEntity product, String bucketURL, int contractStatus) {
        try {
            OrderItemDTO itemDTO = new OrderItemDTO();

            itemDTO.setId(orderItem.getId());
            itemDTO.setTitle(orderItem.getTitle());
            itemDTO.setScript(orderItem.isScript());
            itemDTO.setPerformance(orderItem.isPerformance());
            itemDTO.setContractStatus(orderItem.getContractStatus());

            if(product != null) { // 삭제된 작품이 아닐 경우
                String encodedScriptImage = product.getImagePath() != null ? bucketURL + URLEncoder.encode(product.getImagePath(), "UTF-8") : "";
                int buyPerformance = 0; // 구매 불가능(공연권 계약 중, 공연권 판매 중 아님)

                itemDTO.setDelete(false);
                itemDTO.setWriter(product.getWriter());
                itemDTO.setImagePath(encodedScriptImage);
                itemDTO.setChecked(product.isChecked());
                itemDTO.setScriptPrice(orderItem.isScript() ? product.getScriptPrice() : 0);
                itemDTO.setPerformancePrice(orderItem.isPerformance() ? product.getPerformancePrice() : 0);
                itemDTO.setProductId(product.getId());

                if (product.isPerformance()) { // 공연권 판매 중
                    if (contractStatus == 1) { // 계약 전
                        buyPerformance = 1; // 계약 필요
                    } else if(contractStatus == 3) { // 계약 완료 or 공연권 구매 내역 없음
                        buyPerformance = 2; // 구매 가능
                    }
                }
                
                itemDTO.setBuyPerformance(buyPerformance);               
            } else { // 삭제된 작품일 경우
                itemDTO.setDelete(true);
                itemDTO.setScriptPrice(orderItem.isScript() ? orderItem.getScriptPrice() : 0);
                itemDTO.setPerformancePrice(orderItem.isPerformance() ? orderItem.getPerformancePrice() : 0);
                itemDTO.setBuyPerformance(0);
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
        completeDTO.setScriptPrice(orderItem.isScript() ? orderItem.getScriptPrice() : 0);
        completeDTO.setPerformancePrice(orderItem.isPerformance() ? orderItem.getPerformancePrice() : 0);
        completeDTO.setTotalPrice(ordersEntity.getTotalPrice());

        return completeDTO;
    }
}
