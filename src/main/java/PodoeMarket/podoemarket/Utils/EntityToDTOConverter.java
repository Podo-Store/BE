package PodoeMarket.podoemarket.Utils;

import PodoeMarket.podoemarket.dto.*;
import PodoeMarket.podoemarket.entity.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

@Slf4j
@Component
@RequiredArgsConstructor
public class EntityToDTOConverter {
    static final private String bucketURL = "https://podobucket.s3.ap-northeast-2.amazonaws.com/";

    public static ProductListDTO convertToProductList(ProductEntity entity) {
       try {
           ProductListDTO productListDTO = new ProductListDTO();
           String encodedScriptImage = URLEncoder.encode(entity.getImagePath(), "UTF-8");

           productListDTO.setId(entity.getId());
           productListDTO.setTitle(entity.getTitle());
           productListDTO.setWriter(entity.getWriter());
           productListDTO.setImagePath(bucketURL + encodedScriptImage);
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

    public static ProductDTO convertToSingleProductDTO(ProductEntity entity, boolean isBuyScript) {
        try {
            ProductDTO productDTO = new ProductDTO();
            String encodedScriptImage = URLEncoder.encode(entity.getImagePath(), "UTF-8");
            String encodedFile = URLEncoder.encode(entity.getFilePath(), "UTF-8");
            String encodedDescription = URLEncoder.encode(entity.getDescriptionPath(), "UTF-8");

            productDTO.setId(entity.getId());
            productDTO.setTitle(entity.getTitle());
            productDTO.setWriter(entity.getWriter());
            productDTO.setFilePath(bucketURL + encodedFile);
            productDTO.setImagePath(bucketURL + encodedScriptImage);
            productDTO.setScript(entity.isScript());
            productDTO.setScriptPrice(entity.getScriptPrice());
            productDTO.setPerformance(entity.isPerformance());
            productDTO.setPerformancePrice(entity.getPerformancePrice());
            productDTO.setDescriptionPath(bucketURL + encodedDescription);
            productDTO.setDate(entity.getCreatedAt());
            productDTO.setChecked(entity.isChecked());
            productDTO.setPlayType(entity.getPlayType());

            productDTO.setBuyScript(isBuyScript);

            return productDTO;
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public static OrderItemDTO convertToOrderItemDTO(OrderItemEntity orderItem, ProductEntity product) {
        try {
            String encodedScriptImage = URLEncoder.encode(product.getImagePath(), "UTF-8");
            OrderItemDTO itemDTO = new OrderItemDTO();

            itemDTO.setId(orderItem.getId());
            itemDTO.setTitle(product.getTitle());
            itemDTO.setImagePath(bucketURL + encodedScriptImage);
            itemDTO.setChecked(product.isChecked());
            itemDTO.setScript(orderItem.isScript());
            itemDTO.setScriptPrice(orderItem.isScript() ? product.getScriptPrice() : 0);
            itemDTO.setPerformance(orderItem.isPerformance());
            itemDTO.setPerformancePrice(orderItem.isPerformance() ? product.getPerformancePrice() : 0);
            itemDTO.setContractStatus(orderItem.getContractStatus());

            return itemDTO;
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
}
