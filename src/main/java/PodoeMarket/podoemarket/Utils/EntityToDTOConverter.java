package PodoeMarket.podoemarket.Utils;

import PodoeMarket.podoemarket.dto.*;
import PodoeMarket.podoemarket.entity.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EntityToDTOConverter {

    public static ProductListDTO convertToProductList(ProductEntity entity) {
        ProductListDTO productListDTO = new ProductListDTO();

        productListDTO.setId(entity.getId());
        productListDTO.setTitle(entity.getTitle());
        productListDTO.setWriter(entity.getWriter());
        productListDTO.setImagePath(entity.getImagePath());
        productListDTO.setScript(entity.isScript());
        productListDTO.setScriptPrice(entity.getScriptPrice());
        productListDTO.setPerformance(entity.isPerformance());
        productListDTO.setPerformancePrice(entity.getPerformancePrice());
        productListDTO.setDate(entity.getCreatedAt());
        productListDTO.setChecked(entity.isChecked());

        return productListDTO;
    }

    public static ProductDTO convertToSingleProductDTO(ProductEntity entity) {
        ProductDTO productDTO = new ProductDTO();

        productDTO.setId(entity.getId());
        productDTO.setTitle(entity.getTitle());
        productDTO.setWriter(entity.getWriter());
        productDTO.setFilePath(entity.getFilePath());
        productDTO.setImagePath(entity.getImagePath());
        productDTO.setScript(entity.isScript());
        productDTO.setScriptPrice(entity.getScriptPrice());
        productDTO.setPerformance(entity.isPerformance());
        productDTO.setPerformancePrice(entity.getPerformancePrice());
        productDTO.setDescriptionPath(entity.getDescriptionPath());
        productDTO.setDate(entity.getCreatedAt());
        productDTO.setChecked(entity.isChecked());

        return productDTO;
    }

    public static OrderItemDTO convertToOrderItemDTO(OrderItemEntity orderItem, ProductEntity product) {
        OrderItemDTO itemDTO = new OrderItemDTO();

        itemDTO.setId(orderItem.getId());
        itemDTO.setTitle(product.getTitle());
        itemDTO.setImagePath(product.getImagePath());
        itemDTO.setChecked(product.isChecked());
        itemDTO.setScript(product.isScript());
        itemDTO.setScriptPrice(product.getScriptPrice());
        itemDTO.setPerformance(product.isPerformance());
        itemDTO.setPerformancePrice(product.getPerformancePrice());

        return itemDTO;
    }
}
