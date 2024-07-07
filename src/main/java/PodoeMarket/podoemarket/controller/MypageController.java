package PodoeMarket.podoemarket.controller;

import PodoeMarket.podoemarket.Utils.EntityToDTOConverter;
import PodoeMarket.podoemarket.Utils.ValidUser;
import PodoeMarket.podoemarket.dto.*;
import PodoeMarket.podoemarket.entity.ProductEntity;
import PodoeMarket.podoemarket.entity.QnAEntity;
import PodoeMarket.podoemarket.entity.UserEntity;
import PodoeMarket.podoemarket.service.MailSendService;
import PodoeMarket.podoemarket.service.MypageService;
import PodoeMarket.podoemarket.service.RedisUtil;
import PodoeMarket.podoemarket.service.UserService;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@RequiredArgsConstructor
@Controller
@Slf4j
@RequestMapping("/profile")
public class MypageController {
    private final MypageService mypageService;
    private final MailSendService mailService;
    private final UserService userService;
    private final RedisUtil redisUtil;

    private final PasswordEncoder pwdEncoder = new BCryptPasswordEncoder();

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

            return ResponseEntity.ok().body(user);
        }catch(Exception e) {
            ResponseDTO resDTO = ResponseDTO.builder().error(e.getMessage()).build();
            return ResponseEntity.badRequest().body(resDTO);
        }
    }

    @PostMapping("/checkPw")
    public ResponseEntity<?> checkPassword(@RequestBody UserDTO dto) {
        if(!ValidUser.isValidPw(dto.getPassword())){
            ResponseDTO resDTO = ResponseDTO.builder()
                    .error("비밀번호 유효성 검사 실패")
                    .build();

            return ResponseEntity.badRequest().body(resDTO);
        }

        return ResponseEntity.ok().body(true);
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
    public ResponseEntity<?> checkNickname(@RequestBody UserDTO dto) {
        UserEntity originalUser = mypageService.originalUser(dto.getId());

        // 기존의 닉네임과 다를 경우
        if(!Objects.equals(originalUser.getNickname(), dto.getNickname())) {
            if(!ValidUser.isValidNickname(dto.getNickname())){
                ResponseDTO resDTO = ResponseDTO.builder()
                        .error("닉네임 유효성 검사 실패")
                        .build();

                return ResponseEntity.badRequest().body(resDTO);
            }

            if(userService.checkNickname(dto.getNickname())) {
                ResponseDTO resDTO = ResponseDTO.builder()
                        .error("닉네임 중복")
                        .build();

                return ResponseEntity.badRequest().body(resDTO);
            }
        }

        return ResponseEntity.ok().body(true);
    }

    @PostMapping ("/mailSend")
    public ResponseEntity<?> mailSend(@RequestBody @Valid EmailRequestDTO emailDTO){
        try {
            return ResponseEntity.ok().body(mailService.joinEmail(emailDTO.getEmail()));
        }catch(Exception e) {
            ResponseDTO resDTO = ResponseDTO.builder().error(e.getMessage()).build();
            return ResponseEntity.badRequest().body(resDTO);
        }
    }

    @PostMapping("/mailauthCheck")
    public ResponseEntity<?> AuthCheck(@RequestBody @Valid EmailCheckDTO emailCheckDTO){
        try{
            log.info("Start mailauthCheck");

            boolean Checked = mailService.CheckAuthNum(emailCheckDTO.getEmail(),emailCheckDTO.getAuthNum());

            if(Checked) {
                return ResponseEntity.ok().body(true);
            } else
                throw new NullPointerException("Null Exception");
        } catch(Exception e) {
            ResponseDTO resDTO = ResponseDTO.builder().error(e.getMessage()).build();
            return ResponseEntity.badRequest().body(resDTO);
        }
    }

    @PostMapping("/update")
    public ResponseEntity<?> updateAccount(@AuthenticationPrincipal UserEntity userInfo, UserDTO dto, @RequestParam("image")MultipartFile file) {
        try{
            if(!ValidUser.isValidUser(dto)) {
                ResponseDTO resDTO = ResponseDTO.builder()
                        .error("유효성 검사 실패")
                        .build();

                return ResponseEntity.badRequest().body(resDTO);
            }

            if (!mailService.CheckAuthNum(dto.getEmail(), dto.getAuthNum())) {
                ResponseDTO resDTO = ResponseDTO.builder()
                        .error("이메일 인증 실패")
                        .build();

                return ResponseEntity.badRequest().body(resDTO);
            }

            String filePath = mypageService.uploadUserImage(file);

            UserEntity user = UserEntity.builder()
                    .userId(dto.getUserId())
                    .password(pwdEncoder.encode(dto.getPassword()))
                    .nickname(dto.getNickname())
                    .email(dto.getEmail())
                    .type(file.getContentType())
                    .filePath(filePath)
                    .build();

            // token 값 변경 가능성 있음
            mypageService.userUpdate(userInfo.getId(), user);
            redisUtil.deleteData(dto.getAuthNum()); // 인증 번호 확인 후, redis 상에서 즉시 삭제

            return ResponseEntity.ok().body(true);
        } catch(Exception e) {
            ResponseDTO resDTO = ResponseDTO.builder().error(e.getMessage()).build();
            return ResponseEntity.badRequest().body(resDTO);
        }
    }

    @GetMapping("/script")
    public ResponseEntity<?> scriptList(@AuthenticationPrincipal UserEntity userInfo) {
        try{
            return ResponseEntity.ok().body(mypageService.getAllMyProducts(userInfo.getNickname()));
        } catch(Exception e) {
            ResponseDTO resDTO = ResponseDTO.builder().error(e.getMessage()).build();
            return ResponseEntity.badRequest().body(resDTO);
        }
    }

    @PostMapping("/scriptDetail")
    public ResponseEntity<?> scriptDetail(ProductDTO dto, @RequestParam("image") MultipartFile file) {
        try{
            String filePath = mypageService.uploadScriptImage(file);

            ProductEntity product = ProductEntity.builder()
                    .imagePath(filePath)
                    .imageType(file.getContentType())
                    .genre(dto.getGenre())
                    .characterNumber(dto.getCharacterNumber())
                    .runtime(dto.getRuntime())
                    .title(dto.getTitle())
                    .story(dto.getStory())
                    .script(dto.isScript())
                    .performance(dto.isPerformance())
                    .scriptPrice(dto.getScriptPrice())
                    .performancePrice(dto.getPerformancePrice())
                    .content(dto.getContent())
                    .build();

            mypageService.productUpdate(dto.getId(), product);
            return ResponseEntity.ok().body(true);
        } catch(Exception e) {
            ResponseDTO resDTO = ResponseDTO.builder().error(e.getMessage()).build();
            return ResponseEntity.badRequest().body(resDTO);
        }
    }

    @GetMapping("/like")
    public ResponseEntity<?> likeList(@AuthenticationPrincipal UserEntity userInfo){
        try{
            return ResponseEntity.ok().body(mypageService.getAllLikeProducts(userInfo.getId()));
        } catch(Exception e) {
            ResponseDTO resDTO = ResponseDTO.builder().error(e.getMessage()).build();
            return ResponseEntity.badRequest().body(resDTO);
        }
    }

    @GetMapping("/qna")
    public ResponseEntity<?> getTotalQnA(@AuthenticationPrincipal UserEntity userInfo) {
        try {
            // 페이지네이션 필요
            List<QnADTO> oftenQnA = mypageService.getAllOftenQnA();
            List<QnADTO> myQnA = mypageService.getAllMyQnA(userInfo.getId());

            QnAResponseDTO res = new QnAResponseDTO(oftenQnA, myQnA);

            return ResponseEntity.ok().body(res);
        } catch (Exception e) {
            ResponseDTO resDTO = ResponseDTO.builder().error(e.getMessage()).build();
            return ResponseEntity.badRequest().body(resDTO);
        }
    }

    @PostMapping("/question")
    public ResponseEntity<?> writeQuestion(@AuthenticationPrincipal UserEntity userInfo, @RequestBody QnADTO dto) {
        try {
            QnAEntity question = QnAEntity.builder()
                    .user(userInfo)
                    .question(dto.getQuestion())
                    .build();

            mypageService.writeQuestion(question);

            return ResponseEntity.ok().body("question register");
        } catch (Exception e) {
            ResponseDTO resDTO = ResponseDTO.builder().error(e.getMessage()).build();
            return ResponseEntity.badRequest().body(resDTO);
        }
    }

    @GetMapping("/oftenQnA")
    public ResponseEntity<?> getOftenQnA() {
        try {
            // 페이지네이션 필요
            return ResponseEntity.ok().body(mypageService.getAllOftenQnA());
        } catch (Exception e) {
            ResponseDTO resDTO = ResponseDTO.builder().error(e.getMessage()).build();
            return ResponseEntity.badRequest().body(resDTO);
        }
    }

    @GetMapping("/search")
    public ResponseEntity<?> oftenQnASearch(@RequestParam(value = "keyword", required = false) String keyword) {
        try {
            return ResponseEntity.ok().body(mypageService.getSearchOftenQnA(keyword));
        } catch (Exception e) {
            ResponseDTO resDTO = ResponseDTO.builder().error(e.getMessage()).build();
            return ResponseEntity.badRequest().body(resDTO);
        }
    }

    @GetMapping("/myQnA")
    public ResponseEntity<?> getmyQnA(@AuthenticationPrincipal UserEntity userInfo) {
        try {
            // 페이지네이션 필요
            return ResponseEntity.ok().body(mypageService.getAllMyQnA(userInfo.getId()));
        } catch (Exception e) {
            ResponseDTO resDTO = ResponseDTO.builder().error(e.getMessage()).build();
            return ResponseEntity.badRequest().body(resDTO);
        }
    }

    @GetMapping("/mySearch")
    public ResponseEntity<?> myQnASearch(@RequestParam(value = "keyword", required = false) String keyword, @AuthenticationPrincipal UserEntity userInfo) {
        try {
            return ResponseEntity.ok().body(mypageService.getSearchMyQnA(userInfo.getId(), keyword));
        } catch (Exception e) {
            ResponseDTO resDTO = ResponseDTO.builder().error(e.getMessage()).build();
            return ResponseEntity.badRequest().body(resDTO);
        }
    }
}
