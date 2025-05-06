package PodoeMarket.podoemarket.profile.controller;

import PodoeMarket.podoemarket.Utils.ValidCheck;
import PodoeMarket.podoemarket.common.entity.ProductEntity;
import PodoeMarket.podoemarket.common.entity.UserEntity;
import PodoeMarket.podoemarket.common.entity.type.ProductStatus;
import PodoeMarket.podoemarket.dto.response.ResponseDTO;
import PodoeMarket.podoemarket.product.service.ProductService;
import PodoeMarket.podoemarket.profile.dto.request.DetailUpdateRequestDTO;
import PodoeMarket.podoemarket.profile.dto.response.ScriptDetailResponseDTO;
import PodoeMarket.podoemarket.profile.dto.response.WorkListResponseDTO;
import PodoeMarket.podoemarket.profile.service.WorkService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.text.Normalizer;
import java.util.UUID;

@RequiredArgsConstructor
@RestController
@Slf4j
@RequestMapping("/profile/work")
public class WorkController {
    private final WorkService workService;
    private final ProductService productService;

    @GetMapping("")
    public ResponseEntity<?> getWorks(@AuthenticationPrincipal UserEntity userInfo) {
        try {
            final WorkListResponseDTO resDTO = WorkListResponseDTO.builder()
                    .nickname(userInfo.getNickname())
                    .dateWorks(workService.getDateWorks(userInfo.getId()))
                    .build();

            return ResponseEntity.ok().body(resDTO);
        } catch(Exception e) {
            ResponseDTO resDTO = ResponseDTO.builder().error(e.getMessage()).build();
            return ResponseEntity.badRequest().body(resDTO);
        }
    }

    @GetMapping("/detail")
    public ResponseEntity<?> scriptDetail(@RequestParam("script") UUID productId) {
        try{
            ScriptDetailResponseDTO productInfo = workService.getProductDetail(productId, 0);

            return ResponseEntity.ok().body(productInfo);
        } catch(Exception e) {
            ResponseDTO resDTO = ResponseDTO.builder().error(e.getMessage()).build();
            return ResponseEntity.badRequest().body(resDTO);
        }
    }

    @PostMapping(value = "/detail", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> detailUpdate(DetailUpdateRequestDTO dto,
                                          @RequestPart(value = "scriptImage", required = false) MultipartFile[] file1,
                                          @RequestPart(value = "description", required = false) MultipartFile[] file2) {
        try{
            // 입력 받은 제목을 NFKC 정규화 적용 (전각/반각, 분해형/조합형 등 모든 호환성 문자를 통일)
            String normalizedTitle = Normalizer.normalize(dto.getTitle(), Normalizer.Form.NFKC);

            if(!ValidCheck.isValidTitle(normalizedTitle)){
                ResponseDTO resDTO = ResponseDTO.builder()
                        .error("제목 유효성 검사 실패")
                        .build();

                return ResponseEntity.badRequest().body(resDTO);
            }

            if((productService.getProduct(dto.getId())).getChecked() == ProductStatus.WAIT) {
                ResponseDTO resDTO = ResponseDTO.builder()
                        .error("등록 심사 중인 작품")
                        .build();

                return ResponseEntity.badRequest().body(resDTO);
            }

            if(!ValidCheck.isValidPlot(dto.getPlot())){
                ResponseDTO resDTO = ResponseDTO.builder()
                        .error("줄거리 유효성 검사 실패")
                        .build();

                return ResponseEntity.badRequest().body(resDTO);
            }

            String scriptImageFilePath = null;
            if(file1 != null && file1.length > 0 && !file1[0].isEmpty()) {
                scriptImageFilePath = workService.uploadScriptImage(file1, dto.getTitle(), dto.getId());
            } else if (dto.getImagePath() != null) {
                scriptImageFilePath = workService.extractS3KeyFromURL(dto.getImagePath());
            } else if (dto.getImagePath() == null) {
                workService.setScriptImageDefault(dto.getId());
            }

            String descriptionFilePath = null;
            if(file2 != null && file2.length > 0 && !file2[0].isEmpty()) {
                descriptionFilePath = workService.uploadDescription(file2, dto.getTitle(), dto.getId());
            } else if (dto.getDescriptionPath() != null) {
                descriptionFilePath = workService.extractS3KeyFromURL(dto.getDescriptionPath());
            } else if (dto.getDescriptionPath() == null) {
                workService.setDescriptionDefault(dto.getId());
            }

            ProductEntity product = ProductEntity.builder()
                    .imagePath(scriptImageFilePath)
                    .title(normalizedTitle)
                    .script(dto.getScript())
                    .performance(dto.getPerformance())
                    .scriptPrice(dto.getScriptPrice())
                    .performancePrice(dto.getPerformancePrice())
                    .descriptionPath(descriptionFilePath)
                    .plot(dto.getPlot())
                    .any(dto.getAny())
                    .male(dto.getMale())
                    .female(dto.getFemale())
                    .stageComment(dto.getStageComment())
                    .runningTime(dto.getRunningTime())
                    .scene(dto.getScene())
                    .act(dto.getAct())
                    .build();

            workService.productUpdate(dto.getId(), product);

            return ResponseEntity.ok().body(true);
        } catch(Exception e) {
            ResponseDTO resDTO = ResponseDTO.builder().error(e.getMessage()).build();
            return ResponseEntity.badRequest().body(resDTO);
        }
    }
}
