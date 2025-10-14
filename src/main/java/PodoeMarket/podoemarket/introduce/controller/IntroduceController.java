package PodoeMarket.podoemarket.introduce.controller;

import PodoeMarket.podoemarket.common.dto.ResponseDTO;
import PodoeMarket.podoemarket.introduce.dto.response.StatisticsResponseDTO;
import PodoeMarket.podoemarket.introduce.service.IntroduceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@Slf4j
@RequestMapping("/introduce")
public class IntroduceController {
    private final IntroduceService introduceService;

    @GetMapping("/statistics")
    public ResponseEntity<?> getStatistics() {
        try {
            final StatisticsResponseDTO resDTO = introduceService.getStatistics();

            return ResponseEntity.ok(resDTO);
        } catch (Exception e) {
            ResponseDTO resDTO = ResponseDTO.builder().error(e.getMessage()).build();
            return ResponseEntity.badRequest().body(resDTO);
        }
    }
}
