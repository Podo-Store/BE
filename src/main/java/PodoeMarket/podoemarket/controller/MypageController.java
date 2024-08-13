package PodoeMarket.podoemarket.controller;

import PodoeMarket.podoemarket.Utils.ValidCheck;
import PodoeMarket.podoemarket.dto.*;
import PodoeMarket.podoemarket.entity.ProductEntity;
import PodoeMarket.podoemarket.entity.UserEntity;
import PodoeMarket.podoemarket.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
                    .password(user.getPassword())
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
            ProductDTO productInfo = productService.productDetail(productId);

            return ResponseEntity.ok().body(productInfo);
        } catch(Exception e) {
            ResponseDTO resDTO = ResponseDTO.builder().error(e.getMessage()).build();
            return ResponseEntity.badRequest().body(resDTO);
        }
    }

    @PostMapping("/detail")
    public ResponseEntity<?> detailUpdate(ProductDTO dto, @RequestParam("scriptImage") MultipartFile[] file1, @RequestParam("description") MultipartFile[] file2) {
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

            String scriptImageFilePath = mypageService.uploadScriptImage(file1, dto.getTitle());
            String descriptionFilePath = mypageService.uploadDescription(file2, dto.getTitle());

            ProductEntity product = ProductEntity.builder()
                    .imagePath(scriptImageFilePath)
                    .imageType(file1[0].getContentType())
                    .title(dto.getTitle())
                    .script(dto.isScript())
                    .performance(dto.isPerformance())
                    .scriptPrice(dto.getScriptPrice())
                    .performancePrice(dto.getPerformancePrice())
                    .descriptionPath(descriptionFilePath)
                    .descriptionType(file2[0].getContentType())
                    .build();

            mypageService.productUpdate(dto.getId(), product);

            return ResponseEntity.ok().body(true);
        } catch(Exception e) {
            ResponseDTO resDTO = ResponseDTO.builder().error(e.getMessage()).build();
            return ResponseEntity.badRequest().body(resDTO);
        }
    }

    @GetMapping("/orderItems")
    public ResponseEntity<?> getOrderItems(@AuthenticationPrincipal UserEntity userInfo) {
        try {
            OrderListPageDTO result = OrderListPageDTO.builder()
                    .nickname(userInfo.getNickname())
                    .orderList(mypageService.getAllMyOrdersWithProducts(userInfo.getId()))
                    .build();

            return ResponseEntity.ok().body(result);
        } catch (Exception e) {
            ResponseDTO resDTO = ResponseDTO.builder().error(e.getMessage()).build();
            return ResponseEntity.badRequest().body(resDTO);
        }
    }

    @PostMapping("/mailSend")
    public ResponseEntity<?> mailSend(@AuthenticationPrincipal UserEntity userInfo) {
        try {
            return ResponseEntity.ok().body(mailService.joinEmailWithContract(userInfo.getEmail(), userInfo.getNickname()));
        } catch(Exception e) {
            ResponseDTO resDTO = ResponseDTO.builder().error(e.getMessage()).build();
            return ResponseEntity.badRequest().body(resDTO);
        }
    }
}
