package PodoeMarket.podoemarket.controller;

import PodoeMarket.podoemarket.Utils.ValidCheck;
import PodoeMarket.podoemarket.dto.*;
import PodoeMarket.podoemarket.entity.OrderItemEntity;
import PodoeMarket.podoemarket.entity.ProductEntity;
import PodoeMarket.podoemarket.entity.UserEntity;
import PodoeMarket.podoemarket.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.util.Objects;
import java.util.UUID;

@RequiredArgsConstructor
@Controller
@Slf4j
@RequestMapping("/profile")
public class MypageController {
    private final MypageService mypageService;
    private final UserService userService;
    private final ProductService productService;
    private final MailSendService mailService;

    private final PasswordEncoder pwdEncoder = new BCryptPasswordEncoder();

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
    public ResponseEntity<?> confirmPassword(@AuthenticationPrincipal UserEntity userInfo, @RequestBody UserDTO dto){
        try{
            boolean confirm = mypageService.checkUser(userInfo.getId(), dto.getPassword(), pwdEncoder);
            if(confirm)
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
            UserEntity user = mypageService.originalUser(userInfo.getId());

            UserDTO info = UserDTO.builder()
                    .id(user.getId())
                    .userId(user.getUserId())
                    .nickname(user.getNickname())
                    .email(user.getEmail())
                    .build();

            return ResponseEntity.ok().body(info);
        }catch(Exception e) {
            ResponseDTO resDTO = ResponseDTO.builder().error(e.getMessage()).build();
            return ResponseEntity.badRequest().body(resDTO);
        }
    }

    @PostMapping("/equalPw")
    public ResponseEntity<?> equalPassword(@RequestBody UserDTO dto) {
        if(!dto.getPassword().equals(dto.getConfirmPassword())){
            ResponseDTO resDTO = ResponseDTO.builder()
                    .error("비밀번호 불일치")
                    .build();

            return ResponseEntity.badRequest().body(resDTO);
        }

        return ResponseEntity.ok().body(true);
    }

    @PostMapping("/checkNickname")
    public ResponseEntity<?> checkNickname(@AuthenticationPrincipal UserEntity userInfo, @RequestBody UserDTO dto) {
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
    public ResponseEntity<?> updateAccount(@AuthenticationPrincipal UserEntity userInfo, @RequestBody UserDTO dto) {
        try{
            if(!ValidCheck.isValidPw(dto.getPassword())) {
                ResponseDTO resDTO = ResponseDTO.builder()
                        .error("비밀번호 유효성 검사 실패")
                        .build();

                return ResponseEntity.badRequest().body(resDTO);
            } else if(!ValidCheck.isValidNickname(dto.getNickname())) {
                ResponseDTO resDTO = ResponseDTO.builder()
                        .error("닉네임 유효성 검사 실패")
                        .build();

                return ResponseEntity.badRequest().body(resDTO);
            }

            if(!dto.getPassword().equals(dto.getConfirmPassword())){
                ResponseDTO resDTO = ResponseDTO.builder()
                        .error("비밀번호가 일치하지 않음")
                        .build();
                return ResponseEntity.badRequest().body(resDTO);
            }

            UserEntity user = UserEntity.builder()
                    .userId(dto.getUserId())
                    .password(pwdEncoder.encode(dto.getPassword()))
                    .nickname(dto.getNickname())
                    .email(dto.getEmail())
                    .build();

            mypageService.userUpdate(userInfo.getId(), user);
            mypageService.updateWriter(userInfo.getId(), dto.getNickname());

            return ResponseEntity.ok().body(true);
        } catch(Exception e) {
            ResponseDTO resDTO = ResponseDTO.builder().error(e.getMessage()).build();
            return ResponseEntity.badRequest().body(resDTO);
        }
    }

    @GetMapping("/scripts")
    public ResponseEntity<?> scriptList(@AuthenticationPrincipal UserEntity userInfo) {
        try{
            ProductListPageDTO result = ProductListPageDTO.builder()
                    .nickname(userInfo.getNickname())
                    .productList(mypageService.getAllMyProducts(userInfo.getId()))
                    .build();

            return ResponseEntity.ok().body(result);
        } catch(Exception e) {
            ResponseDTO resDTO = ResponseDTO.builder().error(e.getMessage()).build();
            return ResponseEntity.badRequest().body(resDTO);
        }
    }

    @GetMapping("/detail")
    public ResponseEntity<?> scriptDetail(@RequestParam("script") UUID productId) {
        try{
            ProductDTO productInfo = productService.productDetail(productId, false);

            return ResponseEntity.ok().body(productInfo);
        } catch(Exception e) {
            ResponseDTO resDTO = ResponseDTO.builder().error(e.getMessage()).build();
            return ResponseEntity.badRequest().body(resDTO);
        }
    }

    @PostMapping("/detail")
    public ResponseEntity<?> detailUpdate(ProductDTO dto, @RequestParam(value = "scriptImage", required = false) MultipartFile[] file1, @RequestParam(value = "description", required = false) MultipartFile[] file2) {
        try{
            if(!ValidCheck.isValidTitle(dto.getTitle())){
                ResponseDTO resDTO = ResponseDTO.builder()
                        .error("제목 유효성 검사 실패")
                        .build();

                return ResponseEntity.badRequest().body(resDTO);
            }

            if(!(productService.product(dto.getId())).isChecked()) {
                ResponseDTO resDTO = ResponseDTO.builder()
                        .error("등록 심사 중인 작품")
                        .build();

                return ResponseEntity.badRequest().body(resDTO);
            }

            String scriptImageFilePath;
            if(file1 != null && file1.length > 0 && !file1[0].isEmpty()) {
                scriptImageFilePath = mypageService.uploadScriptImage(file1, dto.getTitle(), dto.getId());
            } else {
                scriptImageFilePath = mypageService.extractS3KeyFromURL(dto.getImagePath());
            }

            String descriptionFilePath;
            if(file2 != null && file2.length > 0 && !file2[0].isEmpty()) {
                descriptionFilePath = mypageService.uploadDescription(file2, dto.getTitle(), dto.getId());
            } else {
                descriptionFilePath = mypageService.extractS3KeyFromURL(dto.getDescriptionPath());
            }

            ProductEntity product = ProductEntity.builder()
                    .imagePath(scriptImageFilePath)
                    .title(dto.getTitle())
                    .script(dto.isScript())
                    .performance(dto.isPerformance())
                    .scriptPrice(dto.getScriptPrice())
                    .performancePrice(dto.getPerformancePrice())
                    .descriptionPath(descriptionFilePath)
                    .build();

            mypageService.productUpdate(dto.getId(), product);

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
            OrderListPageDTO result = OrderListPageDTO.builder()
                    .nickname(userInfo.getNickname())
                    .orderList(mypageService.getAllMyOrderScriptWithProducts(userInfo.getId()))
                    .build();

            return ResponseEntity.ok().body(result);
        } catch (Exception e) {
            ResponseDTO resDTO = ResponseDTO.builder().error(e.getMessage()).build();
            return ResponseEntity.badRequest().body(resDTO);
        }
    }

    @GetMapping("/orderPerformances")
    public ResponseEntity<?> getOrderPerformances(@AuthenticationPrincipal UserEntity userInfo) {
        try {
            OrderListPageDTO result = OrderListPageDTO.builder()
                    .nickname(userInfo.getNickname())
                    .orderList(mypageService.getAllMyOrderPerformanceWithProducts(userInfo.getId()))
                    .build();

            return ResponseEntity.ok().body(result);
        } catch (Exception e) {
            ResponseDTO resDTO = ResponseDTO.builder().error(e.getMessage()).build();
            return ResponseEntity.badRequest().body(resDTO);
        }
    }

    @PostMapping("/mailSend")
    public ResponseEntity<?> mailSend(@AuthenticationPrincipal UserEntity userInfo, @RequestBody OrderItemDTO dto) {
        try {
            final OrderItemEntity orderItem = mypageService.orderItem(dto.getId());
            final int contractStatus = orderItem.getContractStatus();

            if (contractStatus == 0){
                ResponseDTO resDTO = ResponseDTO.builder()
                        .error("공연권 계약이 불가한 작품입니다.")
                        .build();

                return ResponseEntity.badRequest().body(resDTO);
            } else if (contractStatus == 2) {
                ResponseDTO resDTO = ResponseDTO.builder()
                        .error("공연권 계약이 진행 중입니다.")
                        .build();

                return ResponseEntity.badRequest().body(resDTO);
            } else if (contractStatus == 3) {
                ResponseDTO resDTO = ResponseDTO.builder()
                        .error("이미 공연권 계약이 완료되었습니다.")
                        .build();

                return ResponseEntity.badRequest().body(resDTO);
            }

            if (mailService.joinEmailWithContract(userInfo.getEmail(), userInfo.getNickname())) {
                mypageService.contractStatusUpdate(dto.getId());
            }

            return ResponseEntity.ok().body(true);
        } catch(Exception e) {
            ResponseDTO resDTO = ResponseDTO.builder().error(e.getMessage()).build();
            return ResponseEntity.badRequest().body(resDTO);
        }
    }

    @GetMapping("/contract")
    public ResponseEntity<?> readContract(@AuthenticationPrincipal UserEntity userInfo, @RequestParam("id") UUID orderId) {
        try {
            final OrderItemEntity orderItem = mypageService.orderItem(orderId);

            if (!(orderItem.getUser().getId().equals(userInfo.getId()))) {
                ResponseDTO resDTO = ResponseDTO.builder()
                        .error("권한이 없습니다.")
                        .build();

                return ResponseEntity.badRequest().body(resDTO);
            }

            final int contractStatus = orderItem.getContractStatus();

            if (contractStatus != 3) {
                ResponseDTO resDTO = ResponseDTO.builder()
                        .error("공연권 계약이 체결되지 않았습니다.")
                        .build();

                return ResponseEntity.badRequest().body(resDTO);
            }

            return ResponseEntity.ok().body(orderItem.getContractPath());
        } catch (Exception e) {
            ResponseDTO resDTO = ResponseDTO.builder().error(e.getMessage()).build();
            return ResponseEntity.badRequest().body(resDTO);
        }
    }

    @GetMapping(value = "/download", produces = "application/json; charset=UTF-8")
    public ResponseEntity<?> scriptDownload(@AuthenticationPrincipal UserEntity userInfo, @RequestParam("id") UUID orderId) {
        try {
            OrderItemEntity item = mypageService.orderItem(orderId);
            if(!item.isScript()) {
                ResponseDTO resDTO = ResponseDTO.builder()
                        .error("대본을 구매하세요.")
                        .build();

                return ResponseEntity.badRequest().body(resDTO);
            }

            byte[] fileData = mypageService.downloadFile(item.getProduct().getFilePath(), userInfo.getEmail());

            String encodedFilename = URLEncoder.encode(item.getProduct().getTitle(), "UTF-8");

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
}
