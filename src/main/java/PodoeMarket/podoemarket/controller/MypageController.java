package PodoeMarket.podoemarket.controller;

import PodoeMarket.podoemarket.Utils.EntityToDTOConverter;
import PodoeMarket.podoemarket.Utils.ValidCheck;
import PodoeMarket.podoemarket.dto.*;
import PodoeMarket.podoemarket.dto.response.*;
import PodoeMarket.podoemarket.entity.*;
import PodoeMarket.podoemarket.security.TokenProvider;
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
    private final TokenProvider tokenProvider;

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
            if(!dto.getPassword().isBlank() && !dto.getPassword().equals(dto.getConfirmPassword())){
                ResponseDTO resDTO = ResponseDTO.builder()
                        .error("비밀번호가 일치하지 않음")
                        .build();
                return ResponseEntity.badRequest().body(resDTO);
            }

            if(!dto.getPassword().isBlank()) {
                if(!ValidCheck.isValidPw(dto.getPassword())) {
                    ResponseDTO resDTO = ResponseDTO.builder()
                            .error("비밀번호 유효성 검사 실패")
                            .build();
                    return ResponseEntity.badRequest().body(resDTO);
                }
                userInfo.setPassword(pwdEncoder.encode(dto.getPassword()));
            }

            if(!dto.getNickname().isBlank()) {
                if(!ValidCheck.isValidNickname(dto.getNickname())) {
                    ResponseDTO resDTO = ResponseDTO.builder()
                            .error("닉네임 유효성 검사 실패")
                            .build();
                    return ResponseEntity.badRequest().body(resDTO);
                }
                userInfo.setNickname(dto.getNickname());
            }

            UserEntity user = mypageService.updateUser(userInfo);

            final UserDTO resUserDTO = UserDTO.builder()
                    .id(user.getId())
                    .userId(user.getUserId())
                    .email(user.getEmail())
                    .password(user.getPassword())
                    .nickname(user.getNickname())
                    .accessToken(tokenProvider.createAccessToken(user))
                    .refreshToken(tokenProvider.createRefreshToken(user))
                    .build();

            return ResponseEntity.ok().body(resUserDTO);
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

            String scriptImageFilePath = null;
            if(file1 != null && file1.length > 0 && !file1[0].isEmpty()) {
                scriptImageFilePath = mypageService.uploadScriptImage(file1, dto.getTitle(), dto.getId());
            } else if (dto.getImagePath() != null) {
                scriptImageFilePath = mypageService.extractS3KeyFromURL(dto.getImagePath());
            } else if (dto.getImagePath() == null) {
                mypageService.setScriptImageDefault(dto.getId());
            }

            String descriptionFilePath = null;
            if(file2 != null && file2.length > 0 && !file2[0].isEmpty()) {
                descriptionFilePath = mypageService.uploadDescription(file2, dto.getTitle(), dto.getId());
            } else if (dto.getDescriptionPath() != null) {
                descriptionFilePath = mypageService.extractS3KeyFromURL(dto.getDescriptionPath());
            } else if (dto.getDescriptionPath() == null) {
                mypageService.setDescriptionDefault(dto.getId());
            }

            ProductEntity product = ProductEntity.builder()
                    .imagePath(scriptImageFilePath)
                    .title(dto.getTitle())
                    .script(dto.isScript())
                    .performance(dto.isPerformance())
//                    .scriptPrice(dto.getScriptPrice())
                    .scriptPrice(0)
//                    .performancePrice(dto.getPerformancePrice())
                    .performancePrice(0)
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
            OrderScriptListPageDTO result = OrderScriptListPageDTO.builder()
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
            OrderPerformanceListPageDTO result = OrderPerformanceListPageDTO.builder()
                    .nickname(userInfo.getNickname())
                    .orderList(mypageService.getAllMyOrderPerformanceWithProducts(userInfo.getId()))
                    .build();

            return ResponseEntity.ok().body(result);
        } catch (Exception e) {
            ResponseDTO resDTO = ResponseDTO.builder().error(e.getMessage()).build();
            return ResponseEntity.badRequest().body(resDTO);
        }
    }

    @GetMapping("/apply")
    public ResponseEntity<?> showApply(@RequestParam("id") UUID orderItemId) {
        try {
            final OrderItemEntity orderItem = mypageService.getOrderItem(orderItemId);
            final ApplicantEntity applicant = mypageService.getApplicant(orderItemId);

            ApplyDTO applyDTO = EntityToDTOConverter.convertToApplyDTO(orderItem, applicant);

            return ResponseEntity.ok().body(applyDTO);
        } catch (Exception e) {
            ResponseDTO resDTO = ResponseDTO.builder().error(e.getMessage()).build();
            return ResponseEntity.badRequest().body(resDTO);
        }
    }

    @PostMapping("/apply")
    public ResponseEntity<?> apply(@RequestBody ApplyDTO dto) {
        try {
            final OrderItemEntity orderItem = mypageService.getOrderItem(dto.getOrderItemId());
            mypageService.expire(orderItem.getCreatedAt());

            if(dto.getPerformanceDate().size() > (orderItem.getPerformanceAmount() - mypageService.registerDatesNum(dto.getOrderItemId()))) {
                ResponseDTO resDTO = ResponseDTO.builder()
                        .error("공연권 구매량 초과")
                        .build();

                return ResponseEntity.badRequest().body(resDTO);
            }

            if(dto.getPerformanceDate().isEmpty()) {
                ResponseDTO resDTO = ResponseDTO.builder()
                        .error("신청 날짜가 비어있음")
                        .build();

                return ResponseEntity.badRequest().body(resDTO);
            }

            for(PerformanceDateDTO dateDto : dto.getPerformanceDate()) {
                final PerformanceDateEntity date = PerformanceDateEntity.builder()
                        .date(dateDto.getDate())
                        .orderItem(orderItem)
                        .build();

                mypageService.dateRegister(date);
            }

            return ResponseEntity.ok().body(true);
        } catch (Exception e) {
            ResponseDTO resDTO = ResponseDTO.builder().error(e.getMessage()).build();
            return ResponseEntity.badRequest().body(resDTO);
        }
    }

    @GetMapping(value = "/download", produces = "application/json; charset=UTF-8")
    public ResponseEntity<?> scriptDownload(@AuthenticationPrincipal UserEntity userInfo, @RequestParam("id") UUID orderId) {
        try {
            final OrderItemEntity item = mypageService.getOrderItem(orderId);
            if(!item.isScript()) {
                ResponseDTO resDTO = ResponseDTO.builder()
                        .error("대본을 구매하세요.")
                        .build();

                return ResponseEntity.badRequest().body(resDTO);
            }
            mypageService.expire(item.getCreatedAt());

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

    @GetMapping("/refund")
    public ResponseEntity<?> refundInto(@RequestParam("id") UUID orderItemId) {
        try {
            final OrderItemEntity orderItem = mypageService.getOrderItem(orderItemId);

            final int possibleAmount = orderItem.getPerformanceAmount() - mypageService.registerDatesNum(orderItemId);
            final int possiblePrice = orderItem.getProduct().getPerformancePrice() * possibleAmount;

            RefundDTO resDTO = RefundDTO.builder()
                    .scriptImage(orderItem.getProduct().getImagePath())
                    .title(orderItem.getTitle())
                    .writer(orderItem.getProduct().getWriter())
                    .performancePrice(orderItem.getProduct().getPerformancePrice())
                    .orderDate(orderItem.getCreatedAt())
                    .orderNum(orderItem.getOrder().getId())
                    .orderAmount(orderItem.getPerformanceAmount())
                    .orderPrice(orderItem.getPerformancePrice())
                    .possibleAmount(possibleAmount)
                    .possiblePrice(possiblePrice)
                    .build();

            return ResponseEntity.ok().body(resDTO);
        } catch (Exception e) {
            ResponseDTO resDTO = ResponseDTO.builder().error(e.getMessage()).build();
            return ResponseEntity.badRequest().body(resDTO);
        }
    }

    @PostMapping("/refund")
    public ResponseEntity<?> refund(@AuthenticationPrincipal UserEntity userInfo, @RequestBody RefundDTO dto) {
        try {
            final OrderItemEntity orderItem = mypageService.getOrderItem(dto.getOrderItemId());
            final int possibleAmount = orderItem.getPerformanceAmount() - mypageService.registerDatesNum(dto.getOrderItemId());
            final int possiblePrice = orderItem.getProduct().getPerformancePrice() * possibleAmount;
            final int refundPrice = orderItem.getProduct().getPerformancePrice() * dto.getRefundAmount();

            if(dto.getRefundAmount() > possibleAmount || refundPrice > possiblePrice || dto.getRefundAmount() == 0 || refundPrice < 0) {
                ResponseDTO resDTO = ResponseDTO.builder()
                        .error("환불 가능 수량과 가격이 아님")
                        .build();
                return ResponseEntity.badRequest().body(resDTO);
            }

            if(dto.getReason().isEmpty() || dto.getReason().length() > 50) {
                ResponseDTO resDTO = ResponseDTO.builder()
                        .error("환불 사유는 1 ~ 50자까지 가능")
                        .build();
                return ResponseEntity.badRequest().body(resDTO);
            }

            final RefundEntity refund = RefundEntity.builder()
                    .quantity(dto.getRefundAmount())
                    .price(refundPrice)
                    .content(dto.getReason())
                    .order(orderItem.getOrder())
                    .user(userInfo)
                    .build();

            mypageService.refundRegister(refund);

            return ResponseEntity.ok().body(true);
        } catch (Exception e) {
            ResponseDTO resDTO = ResponseDTO.builder().error(e.getMessage()).build();
            return ResponseEntity.badRequest().body(resDTO);
        }
    }
}
