package PodoeMarket.podoemarket.performance.controller;

import PodoeMarket.podoemarket.common.dto.ResponseDTO;
import PodoeMarket.podoemarket.common.entity.UserEntity;
import PodoeMarket.podoemarket.common.entity.type.PerformanceStatus;
import PodoeMarket.podoemarket.performance.dto.request.PerformanceUpdateRequestDTO;
import PodoeMarket.podoemarket.performance.dto.response.PerformanceEditResponseDTO;
import PodoeMarket.podoemarket.performance.dto.response.PerformanceMainResponseDTO;
import PodoeMarket.podoemarket.performance.dto.response.PerformanceStatusResponseDTO;
import PodoeMarket.podoemarket.performance.service.PerformanceService;
import PodoeMarket.podoemarket.performance.dto.request.PerformanceRegisterRequestDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@RestController
@Slf4j
@RequestMapping("/performance")
public class PerformanceController {
    private final PerformanceService performanceService;

    @PostMapping(value = "/register", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> registerPerformance(@AuthenticationPrincipal UserEntity userInfo,
                                                 PerformanceRegisterRequestDTO dto,
                                                 @RequestPart("poster") MultipartFile file) {
        try {
            performanceService.uploadPerformanceInfo(userInfo, dto, file);

            return ResponseEntity.ok().body(true);
        } catch (Exception e) {
            ResponseDTO resDTO = ResponseDTO.builder().error(e.getMessage()).build();
            return ResponseEntity.badRequest().body(resDTO);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> showPerformanceInfo(@AuthenticationPrincipal UserEntity userInfo, @PathVariable UUID id) {
        try {
            final PerformanceEditResponseDTO resDTO = performanceService.getPerformanceInfo(userInfo, id);

            return  ResponseEntity.ok().body(resDTO);
        } catch (Exception e) {
            ResponseDTO resDTO = ResponseDTO.builder().error(e.getMessage()).build();
            return ResponseEntity.badRequest().body(resDTO);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletePerformance(@AuthenticationPrincipal UserEntity userInfo, @PathVariable UUID id) {
        try {
            performanceService.deletePerformanceInfo(userInfo, id);

            return  ResponseEntity.ok().body(true);
        } catch (Exception e) {
            ResponseDTO resDTO = ResponseDTO.builder().error(e.getMessage()).build();
            return ResponseEntity.badRequest().body(resDTO);
        }
    }

    @PatchMapping(value = "/{id}",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updatePerformance(@AuthenticationPrincipal UserEntity userInfo,
                                               @PathVariable UUID id,
                                               PerformanceUpdateRequestDTO dto,
                                               @RequestPart(value = "poster", required = false) MultipartFile file) {
        try {
            performanceService.updatePerformanceInfo(userInfo, id, dto, file);

            return ResponseEntity.ok().body(true);
        } catch (Exception e) {
            ResponseDTO resDTO = ResponseDTO.builder().error(e.getMessage()).build();
            return ResponseEntity.badRequest().body(resDTO);
        }
    }

    @GetMapping("/main/ongoing")
    public ResponseEntity<?> getOngoingPerformanceMain(@AuthenticationPrincipal UserEntity userInfo, @RequestParam(required = false) Boolean isUsed) {
        try {
            final List<PerformanceMainResponseDTO> resDTO = performanceService.getStatusPerformanceMain(PerformanceStatus.ONGOING, isUsed, userInfo);

            return ResponseEntity.ok().body(resDTO);
        } catch (Exception e) {
            ResponseDTO resDTO = ResponseDTO.builder().error(e.getMessage()).build();
            return ResponseEntity.badRequest().body(resDTO);
        }
    }

    @GetMapping("/main/upcoming")
    public ResponseEntity<?> getUpcomingPerformanceMain(@AuthenticationPrincipal UserEntity userInfo, @RequestParam(required = false) Boolean isUsed) {
        try {
            final List<PerformanceMainResponseDTO> resDTO = performanceService.getStatusPerformanceMain(PerformanceStatus.UPCOMING, isUsed, userInfo);

            return ResponseEntity.ok().body(resDTO);
        } catch (Exception e) {
            ResponseDTO resDTO = ResponseDTO.builder().error(e.getMessage()).build();
            return ResponseEntity.badRequest().body(resDTO);
        }
    }


    @GetMapping("/main/past")
    public ResponseEntity<?> getPastPerformanceMain(@AuthenticationPrincipal UserEntity userInfo, @RequestParam(required = false) Boolean isUsed) {
        try {
            final List<PerformanceMainResponseDTO> resDTO = performanceService.getStatusPerformanceMain(PerformanceStatus.PAST, isUsed, userInfo);

            return ResponseEntity.ok().body(resDTO);
        } catch (Exception e) {
            ResponseDTO resDTO = ResponseDTO.builder().error(e.getMessage()).build();
            return ResponseEntity.badRequest().body(resDTO);
        }
    }

    @GetMapping("/")
    public ResponseEntity<?> getPerformanceList(@AuthenticationPrincipal UserEntity userInfo,
                                                @RequestParam PerformanceStatus status,
                                                @RequestParam(required = false) Boolean isUsed,
                                                @RequestParam(value = "page", defaultValue = "0") int page) {
        try {
            final List<PerformanceStatusResponseDTO.PerformanceListDTO> resDTO = performanceService.getPerformanceList(userInfo, status, isUsed, page, 20).getContent();

            return ResponseEntity.ok().body(resDTO);
        } catch (Exception e) {
            ResponseDTO resDTO = ResponseDTO.builder().error(e.getMessage()).build();
            return ResponseEntity.badRequest().body(resDTO);
        }
    }
}
