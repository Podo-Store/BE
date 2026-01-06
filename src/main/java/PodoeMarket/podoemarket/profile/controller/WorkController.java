package PodoeMarket.podoemarket.profile.controller;

import PodoeMarket.podoemarket.common.entity.UserEntity;
import PodoeMarket.podoemarket.common.dto.ResponseDTO;
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

import java.util.UUID;

@RequiredArgsConstructor
@RestController
@Slf4j
@RequestMapping("/profile/work")
public class WorkController {
    private final WorkService workService;

    @GetMapping("")
    public ResponseEntity<?> getWorks(@AuthenticationPrincipal UserEntity userInfo) {
        try {
            final WorkListResponseDTO resDTO = workService.getUserWorks(userInfo);

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
    public ResponseEntity<?> detailUpdate(@AuthenticationPrincipal UserEntity userInfo,
                                          DetailUpdateRequestDTO dto,
                                          @RequestPart(value = "scriptImage", required = false) MultipartFile[] file1,
                                          @RequestPart(value = "description", required = false) MultipartFile[] file2) {
        try{
            workService.updateProductDetail(userInfo, dto, file1, file2);

            return ResponseEntity.ok().body(true);
        } catch(Exception e) {
            ResponseDTO resDTO = ResponseDTO.builder().error(e.getMessage()).build();
            return ResponseEntity.badRequest().body(resDTO);
        }
    }

    @DeleteMapping("/deleteScript/{id}")
    public ResponseEntity<?> deleteScript(@AuthenticationPrincipal UserEntity userInfo, @PathVariable UUID id) {
        try {
            workService.deleteProduct(id, userInfo.getId());

            return ResponseEntity.ok().body(true);
        } catch (Exception e) {
            ResponseDTO resDTO = ResponseDTO.builder().error(e.getMessage()).build();
            return ResponseEntity.badRequest().body(resDTO);
        }
    }

    @DeleteMapping("/cancel/{id}")
    public ResponseEntity<?> cancelRegister(@AuthenticationPrincipal UserEntity userInfo, @PathVariable UUID id) {
        try {
            workService.cancelRegister(id, userInfo.getId());

            return ResponseEntity.ok().body(true);
        } catch (Exception e) {
            ResponseDTO resDTO = ResponseDTO.builder().error(e.getMessage()).build();
            return ResponseEntity.badRequest().body(resDTO);
        }
    }

    @PostMapping(path = "/changeScript/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> changeScript(@AuthenticationPrincipal UserEntity userInfo, @PathVariable UUID id, @RequestPart("script") MultipartFile[] files) {
        try {
            workService.changeScript(id, userInfo, files);

            return ResponseEntity.ok().body(true);
        } catch (Exception e) {
            ResponseDTO resDTO = ResponseDTO.builder().error(e.getMessage()).build();
            return ResponseEntity.badRequest().body(resDTO);
        }
    }
}
