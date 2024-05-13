package PodoeMarket.podoemarket.controller;

import PodoeMarket.podoemarket.dto.RequestDTO;
import PodoeMarket.podoemarket.dto.ResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@RequiredArgsConstructor
@Controller
@Slf4j
@RequestMapping("/wish")
public class WishScriptController {
    @PostMapping("/script")
    public ResponseEntity<?> requestScript(@RequestBody RequestDTO dto) {
        try{
            // 등록

            return ResponseEntity.ok().body(true);
        } catch (Exception e) {
            ResponseDTO resDTO = ResponseDTO.builder().error(e.getMessage()).build();
            return ResponseEntity.badRequest().body(resDTO);
        }
    }
}
