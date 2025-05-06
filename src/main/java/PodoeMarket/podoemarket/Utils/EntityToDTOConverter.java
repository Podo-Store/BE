package PodoeMarket.podoemarket.Utils;

import PodoeMarket.podoemarket.common.entity.ApplicantEntity;
import PodoeMarket.podoemarket.common.entity.OrderItemEntity;
import PodoeMarket.podoemarket.dto.*;
import PodoeMarket.podoemarket.dto.response.*;
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
