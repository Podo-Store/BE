package PodoeMarket.podoemarket.api.controller;

import PodoeMarket.podoemarket.api.dto.response.SignInStatusResponseDTO;
import PodoeMarket.podoemarket.common.dto.ResponseDTO;
import PodoeMarket.podoemarket.common.entity.UserEntity;
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
@RequestMapping("/api")
public class ApiController {

    @GetMapping("/status")
    public ResponseEntity<?> signInStatus(@AuthenticationPrincipal UserEntity userInfo) {
        try {
            boolean isSignIn = userInfo != null;
            final SignInStatusResponseDTO resDTO = new SignInStatusResponseDTO(isSignIn);

            return ResponseEntity.ok().body(resDTO);
        } catch (Exception e) {
            ResponseDTO resDTO = ResponseDTO.builder().error(e.getMessage()).build();
            return ResponseEntity.badRequest().body(resDTO);
        }
    }
}
