package PodoeMarket.podoemarket.performance.controller;

import PodoeMarket.podoemarket.common.dto.ResponseDTO;
import PodoeMarket.podoemarket.common.entity.UserEntity;
import PodoeMarket.podoemarket.performance.dto.request.PerformanceUpdateRequestDTO;
import PodoeMarket.podoemarket.performance.dto.response.PerformanceEditResponseDTO;
import PodoeMarket.podoemarket.performance.dto.response.PerformanceMainResponseDTO;
import PodoeMarket.podoemarket.performance.service.PerformanceService;
import PodoeMarket.podoemarket.performance.dto.request.PerformanceRegisterRequestDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
            performanceService.getPerformanceInfo(userInfo, dto, file);

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

    @GetMapping
    public ResponseEntity<?> getPerformance(@RequestParam(required = false) Boolean ongoingUsed,
                                            @RequestParam(required = false) Boolean upcomingUsed,
                                            @RequestParam(required = false) Boolean pastUsed) {
        try {
            final PerformanceMainResponseDTO resDTO = performanceService.getPerformanceList(ongoingUsed, upcomingUsed, pastUsed);

            return ResponseEntity.ok().body(resDTO);
        } catch (Exception e) {
            ResponseDTO resDTO = ResponseDTO.builder().error(e.getMessage()).build();
            return ResponseEntity.badRequest().body(resDTO);
        }
    }
}
