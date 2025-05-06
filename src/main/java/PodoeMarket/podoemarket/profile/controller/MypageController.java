package PodoeMarket.podoemarket.profile.controller;

import PodoeMarket.podoemarket.Utils.EntityToDTOConverter;
import PodoeMarket.podoemarket.common.entity.*;
import PodoeMarket.podoemarket.common.entity.type.OrderStatus;
import PodoeMarket.podoemarket.common.entity.type.PlayType;
import PodoeMarket.podoemarket.dto.PerformanceDateDTO;
import PodoeMarket.podoemarket.dto.response.*;
import PodoeMarket.podoemarket.profile.dto.response.RequestedPerformanceResponseDTO;
import PodoeMarket.podoemarket.profile.dto.request.*;
import PodoeMarket.podoemarket.profile.dto.response.*;
import PodoeMarket.podoemarket.product.service.ProductService;
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
    private final ProductService productService;

    @Value("${cloud.aws.s3.url}")
    private String bucketURL;

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
            final UserEntity user = mypageService.originalUser(userInfo.getId());

            final ProfileInfoResponseDTO info = ProfileInfoResponseDTO.builder()
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

    @GetMapping("/detail")
    public ResponseEntity<?> scriptDetail(@RequestParam("script") UUID productId) {
        try{
            ScriptDetailResponseDTO productInfo = mypageService.productDetail(productId, 0);

            return ResponseEntity.ok().body(productInfo);
        } catch(Exception e) {
            ResponseDTO resDTO = ResponseDTO.builder().error(e.getMessage()).build();
            return ResponseEntity.badRequest().body(resDTO);
        }
    }

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
            final OrderItemEntity orderItem = mypageService.getOrderItem(orderItemId);

            if(orderItem.getOrder().getOrderStatus() != OrderStatus.PASS) {
                ResponseDTO resDTO = ResponseDTO.builder()
                        .error("결제 상태를 확인해주십시오.")
                        .build();

                return ResponseEntity.badRequest().body(resDTO);
            }

            final ApplicantEntity applicant = mypageService.getApplicant(orderItemId);

            ApplyDTO applyDTO = EntityToDTOConverter.convertToApplyDTO(orderItem, applicant, bucketURL);

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
            if(!item.getScript()) {
                ResponseDTO resDTO = ResponseDTO.builder()
                        .error("대본을 구매하세요.")
                        .build();

                return ResponseEntity.badRequest().body(resDTO);
            }

            if(item.getOrder().getOrderStatus() != OrderStatus.PASS) {
                ResponseDTO resDTO = ResponseDTO.builder()
                        .error("결제 상태를 확인해주십시오.")
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

    @GetMapping("/requested")
    public ResponseEntity<?> getRequestedPerformances(@RequestParam("id") UUID scriptId, @AuthenticationPrincipal UserEntity userInfo) {
        try {
            final RequestedPerformanceResponseDTO.ProductInfo productInfo = mypageService.getProductInfo(scriptId, userInfo);
            final List<RequestedPerformanceResponseDTO.DateRequestedList> list = mypageService.getDateRequestedList(scriptId);

            final RequestedPerformanceResponseDTO performanceList = RequestedPerformanceResponseDTO.builder()
                    .productInfo(productInfo)
                    .dateRequestedList(list)
                    .build();

            return ResponseEntity.ok().body(performanceList);
        } catch (Exception e) {
            ResponseDTO resDTO = ResponseDTO.builder().error(e.getMessage()).build();
            return ResponseEntity.badRequest().body(resDTO);
        }
    }

    @GetMapping("/like")
    public ResponseEntity<?> getLikeList(@AuthenticationPrincipal UserEntity userInfo) {
        try {
            final ScriptListResponseDTO lists = new ScriptListResponseDTO(mypageService.getLikePlayList(0, userInfo, PlayType.LONG, 4),
                    mypageService.getLikePlayList(0, userInfo, PlayType.SHORT, 4));

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
