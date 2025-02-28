package PodoeMarket.podoemarket.profile.controller;

import PodoeMarket.podoemarket.common.entity.UserEntity;
import PodoeMarket.podoemarket.dto.response.ResponseDTO;
import PodoeMarket.podoemarket.profile.dto.WorkListResponseDTO;
import PodoeMarket.podoemarket.profile.service.WorkService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@Slf4j
@RequestMapping("/profile/work")
public class WorkController {
    private final WorkService workService;

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
}
