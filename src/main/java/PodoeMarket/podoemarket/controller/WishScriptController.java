package PodoeMarket.podoemarket.controller;

import PodoeMarket.podoemarket.dto.WishScriptDTO;
import PodoeMarket.podoemarket.dto.ResponseDTO;
import PodoeMarket.podoemarket.entity.UserEntity;
import PodoeMarket.podoemarket.entity.WishScriptEntity;
import PodoeMarket.podoemarket.entity.WishScriptLikeEntity;
import PodoeMarket.podoemarket.service.WishScriptService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@RequiredArgsConstructor
@Controller
@Slf4j
@RequestMapping("/wish")
public class WishScriptController {
    private final WishScriptService wishScriptService;

    @GetMapping
    public ResponseEntity<?> wishScriptlist(@AuthenticationPrincipal UserEntity userInfo) {
        try {
            if (userInfo == null) { // 로그인 중이 아닐 때
                return ResponseEntity.ok().body(wishScriptService.getAllScripts());
            } else { // 로그인 중일 때
                return ResponseEntity.ok().body(wishScriptService.getAllScripts(userInfo.getId()));
            }
        } catch (Exception e) {
            ResponseDTO resDTO = ResponseDTO.builder().error(e.getMessage()).build();
            return ResponseEntity.badRequest().body(resDTO);
        }
    }

    @PostMapping("/script")
    public ResponseEntity<?> requestScript(@AuthenticationPrincipal UserEntity userInfo, @RequestBody WishScriptDTO dto) {
        try{
            if(dto.getContent() == null || dto.getContent().isBlank()) {
                ResponseDTO resDTO = ResponseDTO.builder()
                        .error("내용이 비어있음")
                        .build();

                return ResponseEntity.badRequest().body(resDTO);
            }

            WishScriptEntity req = WishScriptEntity.builder()
                    .content(dto.getContent())
                    .genre(dto.getGenre())
                    .characterNumber(dto.getCharacterNumber())
                    .runtime(dto.getRuntime())
                    .user(userInfo)
                    .build();

            wishScriptService.scriptCreate(req);

            return ResponseEntity.ok().body(true);
        } catch (Exception e) {
            ResponseDTO resDTO = ResponseDTO.builder().error(e.getMessage()).build();
            return ResponseEntity.badRequest().body(resDTO);
        }
    }

    @PostMapping("/like")
    public ResponseEntity<?> wishScriptLike(@AuthenticationPrincipal UserEntity userInfo, @RequestBody WishScriptDTO dto) {
        try{
            boolean isLike = wishScriptService.isLike(userInfo.getId(), dto.getId());

            if (isLike) { // 좋아요 취소
                wishScriptService.delete(userInfo.getId(), dto.getId());

                return ResponseEntity.ok().body("like delete");
            } else { // 좋아요 생성
                WishScriptEntity wishScript = wishScriptService.script(dto.getId());

                WishScriptLikeEntity like = WishScriptLikeEntity.builder()
                        .user(userInfo)
                        .wish_script(wishScript)
                        .build();

                wishScriptService.likeCreate(like);

                return ResponseEntity.ok().body("like success");
            }
        } catch (Exception e) {
            ResponseDTO resDTO = ResponseDTO.builder().error(e.getMessage()).build();
            return ResponseEntity.badRequest().body(resDTO);
        }
    }
}
