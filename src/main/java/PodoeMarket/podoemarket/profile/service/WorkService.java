package PodoeMarket.podoemarket.profile.service;

import PodoeMarket.podoemarket.common.entity.ProductEntity;
import PodoeMarket.podoemarket.common.repository.ProductRepository;
import PodoeMarket.podoemarket.profile.dto.WorkListResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Slf4j
@Service
public class WorkService {
    private final ProductRepository productRepo;

    @Value("${cloud.aws.s3.url}")
    private String bucketURL;

    public List<WorkListResponseDTO.DateWorkDTO> getDateWorks(final UUID userId) throws UnsupportedEncodingException {
        final List<ProductEntity> products = productRepo.findAllByUserId(userId);

        if (products.isEmpty())
            return Collections.emptyList();

        final Map<LocalDate, List<WorkListResponseDTO.DateWorkDTO.WorksDTO>> works = new HashMap<>();

        for (final ProductEntity product : products) {
            final WorkListResponseDTO.DateWorkDTO.WorksDTO worksDTO = WorkListResponseDTO.DateWorkDTO.WorksDTO.builder()
                    .id(product.getId())
                    .title(product.getTitle())
                    .imagePath(product.getImagePath() != null ? bucketURL + URLEncoder.encode(product.getImagePath(), "UTF-8") : "")
                    .script(product.isScript())
                    .scriptPrice(product.getScriptPrice())
                    .performance(product.isPerformance())
                    .performancePrice(product.getPerformancePrice())
                    .checked(product.getChecked())
                    .build();
            final LocalDate date = product.getCreatedAt().toLocalDate();

            // 날짜에 따른 리스트를 초기화하고 추가 - date라는 key가 없으면 만들고, worksDTO를 value로 추가
            works.computeIfAbsent(date, k -> new ArrayList<>()).add(worksDTO);
        }

        return works.entrySet().stream()
                .sorted(Map.Entry.<LocalDate, List<WorkListResponseDTO.DateWorkDTO.WorksDTO>>comparingByKey().reversed()) // 최신 날짜부터 정렬
                .map(entry -> new WorkListResponseDTO.DateWorkDTO(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }
}
