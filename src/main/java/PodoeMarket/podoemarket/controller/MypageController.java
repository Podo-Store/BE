package PodoeMarket.podoemarket.controller;

import PodoeMarket.podoemarket.config.jwt.JwtProperties;
import PodoeMarket.podoemarket.dto.ResponseDTO;
import PodoeMarket.podoemarket.dto.UserDTO;
import PodoeMarket.podoemarket.entity.UserEntity;
import PodoeMarket.podoemarket.security.TokenProvider;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@RequiredArgsConstructor
@Controller
@Slf4j
@RequestMapping("/profile")
public class MypageController {
    @PostMapping("/account")
    public ResponseEntity<?> updateAccount(@AuthenticationPrincipal UserEntity userInfo, @RequestBody UserDTO dto) {
        try{
            log.info("start account update");



//            UserEntity user = UserEntity.builder()
////                    .id(userInfo.getId())
//                    .email(dto.getEmail())
//                    .password(dto.)
//                    .build();

            return ResponseEntity.ok().body(true);
        } catch(Exception e) {
            ResponseDTO resDTO = ResponseDTO.builder().error(e.getMessage()).build();
            return ResponseEntity.badRequest().body(resDTO);
        }
    }
}
