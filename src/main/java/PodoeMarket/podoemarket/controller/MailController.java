package PodoeMarket.podoemarket.controller;

import PodoeMarket.podoemarket.dto.EmailCheckDTO;
import PodoeMarket.podoemarket.dto.EmailRequestDTO;
import PodoeMarket.podoemarket.service.MailSendService;
import PodoeMarket.podoemarket.service.RedisUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class MailController {
    private final MailSendService mailService;

    @Autowired
    private RedisUtil redisUtil;

    @PostMapping ("/mailSend")
    public String mailSend(@RequestBody @Valid EmailRequestDTO emailDTO){
        System.out.println("이메일 인증 요청이 들어옴");
        System.out.println("이메일 인증 이메일 :" + emailDTO.getEmail());
        return mailService.joinEmail(emailDTO.getEmail());
    }
    @PostMapping("/mailauthCheck")
    public String AuthCheck(@RequestBody @Valid EmailCheckDTO emailCheckDTO){
        boolean Checked = mailService.CheckAuthNum(emailCheckDTO.getEmail(),emailCheckDTO.getAuthNum());

        if(Checked) {
            redisUtil.deleteData(emailCheckDTO.getAuthNum()); // 인증 번호 확인 후, redis 상에서 즉시 삭제
            return "ok";
        }
        else
            throw new NullPointerException("뭔가 잘못!");
    }
}