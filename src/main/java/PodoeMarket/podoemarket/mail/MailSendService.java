package PodoeMarket.podoemarket.mail;

import PodoeMarket.podoemarket.service.RedisUtil;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.util.Random;

@RequiredArgsConstructor
@Slf4j
@Service
public class MailSendService {
    private final JavaMailSender mailSender;
    private final RedisUtil redisUtil;
    private int authNumber;

    @Value("${spring.mail.username}")
    private String username;

    public boolean CheckAuthNum(String email,String authNum){
        if(redisUtil.getData(authNum) == null)
            return false;
        else return redisUtil.getData(authNum).equals(email);
    }

    // 임의의 6자리 양수를 반환
    public void makeRandomNumber() {
        Random r = new Random();
        String randomNumber = "";

        for(int i = 0; i < 6; i++)
            randomNumber += Integer.toString(r.nextInt(9) + 1);

        authNumber = Integer.parseInt(randomNumber);
    }


    // 인증번호
    public String joinEmail(String email) throws Exception {
        makeRandomNumber();
        String setFrom = username; // email-config에 설정한 자신의 이메일 주소를 입력
        String title = "[포도상점] 이메일 인증번호를 보내드립니다."; // 이메일 제목
        String content =
                "<table align=\"center\" width=\"50%\">" +
                        "<tr>" +
                        "<td align=\"center\" style=\"background-color: #f5f0ff\">" +
                        "<!-- 로고 영역 -->" +
                        "<div style=\"margin: 50px\">" +
                        "<img src=\"https://api.podo-store.com/mailLogo.png\" " +
                        "alt=\"포도상점 로고\" " +
                        "style=\"width: 100px; height: auto; display: block\"/>" +
                        "</div>" +
                        "</td>" +
                        "</tr>" +
                        "<tr>" +
                        "<td align=\"left\" style=\"background-color: #ffffff; padding: 40px\">" +
                        "<!-- 본문 내용 -->" +
                        "<p style=\"font-size: 18px; margin-bottom: 20px; color: black\">" +
                        "안녕하세요, 포도상점에서 요청하신 인증번호를 보내드립니다." +
                        "</p>" +
                        "<p style=\"font-size: 32px; font-weight: bold; margin: 50px\">" +
                        authNumber +
                        "</p>" +
                        "<p style=\"font-size: 16px; margin-bottom: 20px; color: black\">" +
                        "위에 안내된 인증번호를 정확히 입력창에 입력해주세요." +
                        "</p>" +
                        "<p style=\"font-size: 14px; color: #777\">" +
                        "인증번호를 요청하지 않으신 경우 본 이메일을 무시해주세요.<br />" +
                        "Please ignore this email if you did not request a verification code." +
                        "</p>" +
                        "</td>" +
                        "</tr>" +
                        "<tr>" +
                        "<td align=\"left\" style=\"padding: 20px; background-color: #f5f0ff\">" +
                        "<!-- 연락처 정보 -->" +
                        "<div style=\"font-size: 14px; color: black\">" +
                        "<p>Contact" +
                        "<br />" +
                        "Email: podostore1111@gmail.com" +
                        "<br />" +
                        "Instagram: @podosangjeom" +
                        "<br />" +
                        "Web: www.podo-store.com" +
                        "</p>" +
                        "<p style=\"margin-top: 20px\">Podo Store © All Rights Reserved</p>" +
                        "</div>" +
                        "</td>" +
                        "</tr>" +
                        "</table>";

        mailSend(setFrom, email, title, content);

        return Integer.toString(authNumber);
    }

    // 결제 요청
    public void joinPaymentEmail(String email, String price) {
        String setFrom = username;
        String title = "[포도상점] 주문하신 상품의 결제 요청드립니다.";
        String content =
                "안녕하세요 포도상점입니다." +
                        "<br>" +
                        "주문하신 상품의 결제 요청드립니다." +
                        "<br><br>" +
                        "결제 요청 금액" +
                        "<br>" +
                        price + "원" +
                        "<br><br>" +
                        "이체 계좌" +
                        "<br>" +
                        "토스뱅크 1001-5507-3197" +
                        "<br><br>" +
                        "*알파 버전 기간동안 입금 금액은 전액 모두 작가님께 지급됩니다." +
                        "<br>" +
                        "*입금이 확인되면 마이페이지 > 구매한 작품 탭에서 작품 사용이 가능합니다." +
                        "<br><br>" +
                        "감사합니다.";
        mailSend(setFrom, email, title, content);
    }

    // 작품 등록 신청 완료
    public void joinRegisterEmail(String email) {
        String setFrom = username;
        String title = "[포도상점] 작품 등록 신청이 완료되었습니다.";
        String content =
                "안녕하세요 포도상점입니다." +
                        "<br>" +
                        "작품 등록 신청이 완료되었습니다." +
                        "<br>" +
                        "심사는 3~5일이 소요되며, 심사 완료 시 메일로 결과를 발송해드립니다." +
                        "<br><br>" +
                        "감사합니다.";
        mailSend(setFrom, email, title, content);
    }

    // 결제 취소
    public void joinCancelEmail(String email, String productTitle) {
        String setFrom = username;
        String title = "[포도상점] 주문하신 상품의 결제가 취소되었습니다.";
        String content =
                "안녕하세요 포도상점입니다." +
                        "<br>" +
                        "주문하신 상품의 결제가 확인되지 않아 취소되었습니다." +
                        "<br><br>" +
                        "취소 상품 명 :" + productTitle +
                        "<br><br>" +
                        "문제가 있으실 경우 포도상점 메일을 통해 문의주시면 감사하겠습니다." +
                        "<br>" +
                        "podostore1111@gmail.com";

        mailSend(setFrom, email, title, content);
    }

    // 이메일 전송
    public void mailSend(String setFrom, String toMail, String title, String content) {
        MimeMessage message = mailSender.createMimeMessage();//JavaMailSender 객체를 사용하여 MimeMessage 객체를 생성
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message,true,"utf-8");//이메일 메시지와 관련된 설정을 수행합니다.
            // true를 전달하여 multipart 형식의 메시지를 지원하고, "utf-8"을 전달하여 문자 인코딩을 설정
            helper.setFrom(setFrom, "포도상점");//이메일의 발신자 주소 설정
            helper.setTo(toMail);//이메일의 수신자 주소 설정
            helper.setSubject(title);//이메일의 제목을 설정
            helper.setText(content,true);//이메일의 내용 설정 두 번째 매개 변수에 true를 설정하여 html 설정으로한다.
            mailSender.send(message);
        } catch (MessagingException e) {//이메일 서버에 연결할 수 없거나, 잘못된 이메일 주소를 사용하거나, 인증 오류가 발생하는 등 오류
            // 이러한 경우 MessagingException이 발생
            e.printStackTrace();//e.printStackTrace()는 예외를 기본 오류 스트림에 출력하는 메서드
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        redisUtil.setDataExpire(Integer.toString(authNumber),toMail,60*5L);
    }
}
