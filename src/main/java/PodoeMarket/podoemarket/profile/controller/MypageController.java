package PodoeMarket.podoemarket.profile.controller;

import PodoeMarket.podoemarket.common.entity.*;
import PodoeMarket.podoemarket.common.entity.type.PlayType;
import PodoeMarket.podoemarket.dto.ResponseDTO;
import PodoeMarket.podoemarket.profile.dto.response.RequestedPerformanceResponseDTO;
import PodoeMarket.podoemarket.profile.dto.request.*;
import PodoeMarket.podoemarket.profile.dto.response.*;
import PodoeMarket.podoemarket.profile.service.MypageService;
import PodoeMarket.podoemarket.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@RequiredArgsConstructor
@RestController
@Slf4j
@RequestMapping("/profile")
public class MypageController {
    private final MypageService mypageService;
    private final UserService userService;

    @GetMapping("/confirm")
    public ResponseEntity<?> getNickname(@AuthenticationPrincipal UserEntity userInfo) {
        try{
            return ResponseEntity.ok().body(userInfo.getNickname());
        } catch(Exception e) {
            ResponseDTO resDTO = ResponseDTO.builder().error(e.getMessage()).build();
            return ResponseEntity.badRequest().body(resDTO);
        }
    }

    @PostMapping("/confirm")
    public ResponseEntity<?> confirmPassword(@AuthenticationPrincipal UserEntity userInfo, @RequestBody EnterCheckRequestDTO dto){
        try{
            if(mypageService.checkUser(userInfo.getId(), dto.getPassword()))
                return ResponseEntity.ok().body(true);
            else{
                ResponseDTO resDTO = ResponseDTO.builder()
                        .error("비밀번호 불일치")
                        .build();

                return ResponseEntity.badRequest().body(resDTO);
            }
        } catch(Exception e) {
            ResponseDTO resDTO = ResponseDTO.builder().error(e.getMessage()).build();
            return ResponseEntity.badRequest().body(resDTO);
        }
    }

    @GetMapping("/account")
    public ResponseEntity<?> account(@AuthenticationPrincipal UserEntity userInfo){
        try {
            final ProfileInfoResponseDTO profile = mypageService.getProfileInfo(userInfo.getId());

            return ResponseEntity.ok().body(profile);
        }catch(Exception e) {
            ResponseDTO resDTO = ResponseDTO.builder().error(e.getMessage()).build();
            return ResponseEntity.badRequest().body(resDTO);
        }
    }

    @PostMapping("/equalPw")
    public ResponseEntity<?> equalPassword(@RequestBody PwCheckRequestDTO dto) {
        if(!dto.getPassword().equals(dto.getConfirmPassword())){
            ResponseDTO resDTO = ResponseDTO.builder()
                    .error("비밀번호 불일치")
                    .build();

            return ResponseEntity.badRequest().body(resDTO);
        }

        return ResponseEntity.ok().body(true);
    }

    @PostMapping("/checkNickname")
    public ResponseEntity<?> checkNickname(@AuthenticationPrincipal UserEntity userInfo, @RequestBody NicknameCheckRequestDTO dto) {
        // 기존의 닉네임과 다를 경우
        if(!Objects.equals(userInfo.getNickname(), dto.getNickname())) {
            if(userService.checkNickname(dto.getNickname())) {
                ResponseDTO resDTO = ResponseDTO.builder()
                        .error("닉네임 중복")
                        .build();

                return ResponseEntity.badRequest().body(resDTO);
            }
        }

        return ResponseEntity.ok().body(true);
    }

    @PostMapping("/update")
    public ResponseEntity<?> updateAccount(@AuthenticationPrincipal UserEntity userInfo, @RequestBody ProfileUpdateRequestDTO dto) {
        try{
            UserInfoResponseDTO resUserDTO = mypageService.updateUserAccount(userInfo, dto);

            return ResponseEntity.ok().body(resUserDTO);
        } catch(Exception e) {
            ResponseDTO resDTO = ResponseDTO.builder().error(e.getMessage()).build();
            return ResponseEntity.badRequest().body(resDTO);
        }
    }

    // 삭제 예정
    @GetMapping("/detail")
    public ResponseEntity<?> scriptDetail(@RequestParam("script") UUID productId) {
        try{
            final ScriptDetailResponseDTO productInfo = mypageService.productDetail(productId, 0);

            return ResponseEntity.ok().body(productInfo);
        } catch(Exception e) {
            ResponseDTO resDTO = ResponseDTO.builder().error(e.getMessage()).build();
            return ResponseEntity.badRequest().body(resDTO);
        }
    }

    // 삭제 예정
    @PostMapping("/detail")
    public ResponseEntity<?> detailUpdate(DetailUpdateRequestDTO dto,
                                          @RequestParam(value = "scriptImage", required = false) MultipartFile[] file1,
                                          @RequestParam(value = "description", required = false) MultipartFile[] file2) {
        try{
            mypageService.updateProductDetail(dto, file1, file2);

            return ResponseEntity.ok().body(true);
        } catch(Exception e) {
            ResponseDTO resDTO = ResponseDTO.builder().error(e.getMessage()).build();
            return ResponseEntity.badRequest().body(resDTO);
        }
    }

    @DeleteMapping("/deleteScript/{id}")
    public ResponseEntity<?> deleteScript(@AuthenticationPrincipal UserEntity userInfo, @PathVariable UUID id) {
        try {
            mypageService.deleteProduct(id, userInfo.getId());

            return ResponseEntity.ok().body(true);
        } catch (Exception e) {
            ResponseDTO resDTO = ResponseDTO.builder().error(e.getMessage()).build();
            return ResponseEntity.badRequest().body(resDTO);
        }
    }

    @GetMapping("/orderScripts")
    public ResponseEntity<?> getOrderScripts(@AuthenticationPrincipal UserEntity userInfo) {
        try {
            OrderScriptsResponseDTO result = mypageService.getUserOrderScripts(userInfo);

            return ResponseEntity.ok().body(result);
        } catch (Exception e) {
            ResponseDTO resDTO = ResponseDTO.builder().error(e.getMessage()).build();
            return ResponseEntity.badRequest().body(resDTO);
        }
    }

    @GetMapping("/orderPerformances")
    public ResponseEntity<?> getOrderPerformances(@AuthenticationPrincipal UserEntity userInfo) {
        try {
            OrderPerformanceResponseDTO result = mypageService.getUserOrderPerformances(userInfo);

            return ResponseEntity.ok().body(result);
        } catch (Exception e) {
            ResponseDTO resDTO = ResponseDTO.builder().error(e.getMessage()).build();
            return ResponseEntity.badRequest().body(resDTO);
        }
    }

    @GetMapping("/apply")
    public ResponseEntity<?> showApply(@RequestParam("id") UUID orderItemId) {
        try {
            ApplyResponseDTO applyDTO = mypageService.getApplyInfo(orderItemId);

            return ResponseEntity.ok().body(applyDTO);
        } catch (Exception e) {
            ResponseDTO resDTO = ResponseDTO.builder().error(e.getMessage()).build();
            return ResponseEntity.badRequest().body(resDTO);
        }
    }

    @PostMapping("/apply")
    public ResponseEntity<?> apply(@RequestBody ApplyRequestDTO dto) {
        try {
            mypageService.processPerformanceApplication(dto);

            return ResponseEntity.ok().body(true);
        } catch (Exception e) {
            ResponseDTO resDTO = ResponseDTO.builder().error(e.getMessage()).build();
            return ResponseEntity.badRequest().body(resDTO);
        }
    }

    @GetMapping(value = "/download", produces = "application/json; charset=UTF-8")
    public ResponseEntity<?> scriptDownload(@AuthenticationPrincipal UserEntity userInfo, @RequestParam("id") UUID orderId) {
        try {
            ScriptInfoResponseDTO scriptInfo = mypageService.checkValidation(orderId);

            byte[] fileData = mypageService.downloadFile(scriptInfo.getFilePath(), userInfo.getEmail());

            String encodedFilename = URLEncoder.encode(scriptInfo.getTitle(), StandardCharsets.UTF_8);

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF) // PDF 파일 형식으로 설정
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFilename)
                    .body(fileData);
        } catch (Exception e) {
            ResponseDTO resDTO = ResponseDTO.builder().error(e.getMessage()).build();
            return ResponseEntity.badRequest().body(resDTO);
        }
    }

    @DeleteMapping("/deleteUser")
    public ResponseEntity<?> deleteUser(@AuthenticationPrincipal UserEntity userInfo) {
        try {
            mypageService.deleteUser(userInfo);

            return ResponseEntity.ok().body(userInfo.getNickname() + "의 계정 삭제");
        } catch (Exception e) {
            ResponseDTO resDTO = ResponseDTO.builder().error(e.getMessage()).build();
            return ResponseEntity.badRequest().body(resDTO);
        }
    }

    @GetMapping("/refund")
    public ResponseEntity<?> refundInfo(@RequestParam("id") UUID orderItemId) {
        try {
            RefundResponseDTO resDTO = mypageService.getRefundInfo(orderItemId);

            return ResponseEntity.ok().body(resDTO);
        } catch (Exception e) {
            ResponseDTO resDTO = ResponseDTO.builder().error(e.getMessage()).build();
            return ResponseEntity.badRequest().body(resDTO);
        }
    }

    @PostMapping("/refund")
    public ResponseEntity<?> refund(@AuthenticationPrincipal UserEntity userInfo, @RequestBody RefundRequestDTO dto) {
        try {
            mypageService.refundProcess(userInfo, dto);

            return ResponseEntity.ok().body(true);
        } catch (Exception e) {
            ResponseDTO resDTO = ResponseDTO.builder().error(e.getMessage()).build();
            return ResponseEntity.badRequest().body(resDTO);
        }
    }

    @GetMapping("/requested")
    public ResponseEntity<?> getRequestedPerformances(@RequestParam("id") UUID productId, @AuthenticationPrincipal UserEntity userInfo) {
        try {
            RequestedPerformanceResponseDTO performanceList = mypageService.getRequestedPerformances(productId, userInfo);

            return ResponseEntity.ok().body(performanceList);
        } catch (Exception e) {
            ResponseDTO resDTO = ResponseDTO.builder().error(e.getMessage()).build();
            return ResponseEntity.badRequest().body(resDTO);
        }
    }

    @GetMapping("/like")
    public ResponseEntity<?> getLikeList(@AuthenticationPrincipal UserEntity userInfo) {
        try {
            final ScriptListResponseDTO lists = mypageService.getUserLikeList(userInfo);

            return ResponseEntity.ok().body(lists);
        } catch (Exception e) {
            ResponseDTO resDTO = ResponseDTO.builder().error(e.getMessage()).build();
            return ResponseEntity.badRequest().body(resDTO);
        }
    }

    @GetMapping("/like/long")
    public ResponseEntity<?> longLikeList(@AuthenticationPrincipal UserEntity userInfo, @RequestParam(value = "page", defaultValue = "0") int page) {
        try {
            final List<ScriptListResponseDTO.ProductListDTO> lists = mypageService.getLikePlayList(page, userInfo, PlayType.LONG, 10);

            return ResponseEntity.ok().body(lists);
        } catch (Exception e) {
            ResponseDTO resDTO = ResponseDTO.builder().error(e.getMessage()).build();
            return ResponseEntity.badRequest().body(resDTO);
        }
    }

    @GetMapping("/like/short")
    public ResponseEntity<?> shortLikeList(@AuthenticationPrincipal UserEntity userInfo,  @RequestParam(value = "page", defaultValue = "0") int page) {
        try {
            final List<ScriptListResponseDTO.ProductListDTO> lists = mypageService.getLikePlayList(0, userInfo, PlayType.SHORT, 10);

            return ResponseEntity.ok().body(lists);
        } catch (Exception e) {
            ResponseDTO resDTO = ResponseDTO.builder().error(e.getMessage()).build();
            return ResponseEntity.badRequest().body(resDTO);
        }
    }
}
