package PodoeMarket.podoemarket.service;

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
    private final VerificationService verificationService;
    private int authNumber;

    @Value("${spring.mail.username}")
    private String username;

    public boolean CheckAuthNum(final String email, final String authNum){
        if(verificationService.getData(authNum) == null)
            return false;
        else return verificationService.getData(authNum).equals(email);
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
    public String joinEmail(final String email) {
        makeRandomNumber();
        String setFrom = username; // email-config에 설정한 자신의 이메일 주소를 입력
        String title = "[포도상점] 이메일 인증번호를 보내드립니다."; // 이메일 제목
        String content =
                "<table align=\"center\" width=\"600px\" height=\"490px\"" +
                        "<tr>" +
                        "<td align=\"center\" style=\"background-color: #f5f0ff\">" +
                        "<!-- 로고 영역 -->" +
                        "<div style=\"margin-top: 40px; margin-bottom: 44.45px\">" +
                        "<img src=\"https://api.podo-store.com/mailLogo.png\" " +
                        "alt=\"포도상점 로고\" " +
                        "style=\"width: 118.93px; height: 32.05px; display: block\"/>" +
                        "</div>" +
                        "</td>" +
                        "</tr>" +
                        "<tr>" +
                        "<td align=\"left\" style=\"background-color: #ffffff; height: 247px\">" +
                        "<!-- 본문 내용 -->" +
                        "<p style=\"font-size: 12.5px; margin-bottom: 20px; margin-left: 35.5px; margin-top: 38px; color: black\">" +
                        "안녕하세요, 포도상점에서 요청하신 인증번호를 보내드립니다." +
                        "</p>" +
                        "<p style=\"font-size: 15px; font-weight: 900; margin-left: 57px; margin-top: 48.5px; margin-bottom: 48.5px\">" +
                        authNumber +
                        "</p>" +
                        "<p style=\"font-size: 12.5px; margin-bottom: 12.5px; margin-left: 35.5px; color: black\">" +
                        "위에 안내된 인증번호를 정확히 입력창에 입력해주세요." +
                        "</p>" +
                        "<p style=\"font-size: 9px; color: #777; margin-left: 35.5px; margin-bottom: 36.5px; line-height: 14px\">" +
                        "인증번호를 요청하지 않으신 경우 본 이메일을 무시해주세요.<br />" +
                        "Please ignore this email if you did not request a verification code." +
                        "</p>" +
                        "</td>" +
                        "</tr>" +
                        "<tr>" +
                        "<td align=\"left\" style=\"background-color: #f5f0ff; height: 121.5px\">" +
                        "<!-- 연락처 정보 -->" +
                        "<div style=\"font-size: 10px; color: black\">" +
                        "<p style=\"color: black; margin-left: 14px; line-height: 16px\">Contact" +
                        "<br />" +
                        "Email: podostore1111@gmail.com" +
                        "<br />" +
                        "Instagram: <a href=\"https://www.instagram.com/podosangjeom/\" style=\"text-decoration:none;\">@podosangjeom</a>" +
                        "<br />" +
                        "Web: www.podo-store.com" +
                        "</p>" +
                        "<p style=\"margin-top: 10px; margin-left: 14px; line-height: 16px\">Podo Store © All Rights Reserved</p>" +
                        "</div>" +
                        "</td>" +
                        "</tr>" +
                        "</table>";

        mailSend(setFrom, email, title, content);

        return Integer.toString(authNumber);
    }

    // 결제 요청
    public void joinPaymentEmail(final String email, final String price) {
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
    public void joinRegisterEmail(final String email, final String scriptTitle) {
        String setFrom = username;
        String title = "[포도상점] 작품 등록 신청이 완료되었습니다.";
        String content =
                "<table align=\"center\" width=\"600px\" height=\"490px\"" +
                        "<tr>" +
                        "<td align=\"center\" style=\"background-color: #f5f0ff\">" +
                        "<!-- 로고 영역 -->" +
                        "<div style=\"margin-top: 40px; margin-bottom: 44.45px\">" +
                        "<img src=\"https://api.podo-store.com/mailLogo.png\" " +
                        "alt=\"포도상점 로고\" " +
                        "style=\"width: 118.93px; height: 32.05px; display: block\"/>" +
                        "</div>" +
                        "</td>" +
                        "</tr>" +
                        "<tr>" +
                        "<td align=\"left\" style=\"background-color: #ffffff; height: 247px\">" +
                        "<!-- 본문 내용 -->" +
                        "<p style=\"font-size: 12.5px; margin-bottom: 20px; margin-left: 35.5px; margin-top: 38px; color: black\">" +
                        "작품 등록 신청이 완료되었습니다." +
                        "</p>" +
                        "<p style=\"font-size: 15px; font-weight: 900; margin-left: 57px; margin-top: 48.5px; margin-bottom: 48.5px\">" +
                        scriptTitle +
                        "</p>" +
                        "<p style=\"font-size: 12.5px; margin-bottom: 12.5px; margin-left: 35.5px; color: black\">" +
                        "심사는 3~5일 소요되며, 심사 완료 시 메일 결과를 발송해드립니다." +
                        "</p>" +
                        "<p style=\"font-size: 9px; color: #777; margin-left: 35.5px; margin-bottom: 36.5px; line-height: 14px\">" +
                        "작품 등록 신청을 취소하시려면 아래 이메일로 문의해주세요.<br />" +
                        "If you wish to cancel your submission, please contact us via the email below." +
                        "</p>" +
                        "</td>" +
                        "</tr>" +
                        "<tr>" +
                        "<td align=\"left\" style=\"background-color: #f5f0ff; height: 121.5px\">" +
                        "<!-- 연락처 정보 -->" +
                        "<div style=\"font-size: 10px; color: black\">" +
                        "<p style=\"color: black; margin-left: 14px; line-height: 16px\">Contact" +
                        "<br />" +
                        "Email: podostore1111@gmail.com" +
                        "<br />" +
                        "Instagram: <a href=\"https://www.instagram.com/podosangjeom/\" style=\"text-decoration:none;\">@podosangjeom</a>" +
                        "<br />" +
                        "Web: www.podo-store.com" +
                        "</p>" +
                        "<p style=\"margin-top: 10px; margin-left: 14px; line-height: 16px\">Podo Store © All Rights Reserved</p>" +
                        "</div>" +
                        "</td>" +
                        "</tr>" +
                        "</table>";

        mailSend(setFrom, email, title, content);
    }

    // 결제 취소
    public void joinCancelEmail(final String email, final String productTitle) {
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

    // 회원가입 완료
    public void joinSignupEmail(final String email) {
        String setFrom = username;
        String title = "[포도상점] 회원가입이 완료되었습니다.";
        String content =
                "<table align=\"center\" width=\"600px\" height=\"490px\"" +
                        "<tr>" +
                        "<td align=\"center\" style=\"background-color: #f5f0ff\">" +
                        "<!-- 로고 영역 -->" +
                        "<div style=\"margin-top: 40px; margin-bottom: 44.45px\">" +
                        "<img src=\"https://api.podo-store.com/mailLogo.png\" " +
                        "alt=\"포도상점 로고\" " +
                        "style=\"width: 118.93px; height: 32.05px; display: block\"/>" +
                        "</div>" +
                        "</td>" +
                        "</tr>" +
                        "<tr>" +
                        "<td align=\"center\" style=\"background-color: #ffffff; height: 247px\">" +
                        "<!-- 본문 내용 -->" +
                        "<p style=\"font-size: 12.5px; margin-bottom: 54px; margin-top: 38px; color: black\">" +
                        "회원가입이 완료되었습니다." +
                        "</p>" +
                        "<p style=\"font-size: 15px; font-weight: 700; color: black; margin-top: 54px; margin-bottom: 54px\">" +
//                        "<p style=\"font-size: 15px; font-weight: 600; color: black; margin-top: 54px; margin-bottom: 54px\">" +
                        "포도상점에서 다양한 작품을 둘러보세요!" +
                        "</p>" +
                        "<table role=\"presentation\" style=\"width: 150px; height: 35px; background: #6a39c0; border-radius: 4.5px; margin-bottom: 38px; text-align: center;\">" +
                        "<tr>" +
                        "<td style=\"font-size: 12.5px; font-weight: 400; padding: 3px 0; border-radius: 4.5px;\">" +
                        "<a href=\"https://www.podo-store.com/list\" style=\"display: block; text-decoration: none; color: #ffffff\">" +
                        "작품 둘러보기" +
                        "</a>" +
                        "</td>" +
                        "</tr>" +
                        "</table>" +
                        "</td>" +
                        "</tr>" +
                        "<tr>" +
                        "<td align=\"left\" style=\"background-color: #f5f0ff; height: 121.5px\">" +
                        "<!-- 연락처 정보 -->" +
                        "<div style=\"font-size: 10px; color: black\">" +
                        "<p style=\"color: black; margin-left: 14px; line-height: 16px\">Contact" +
                        "<br />" +
                        "Email: podostore1111@gmail.com" +
                        "<br />" +
                        "Instagram: <a href=\"https://www.instagram.com/podosangjeom/\" style=\"text-decoration:none;\">@podosangjeom</a>" +
                        "<br />" +
                        "Web: www.podo-store.com" +
                        "</p>" +
                        "<p style=\"margin-top: 10px; margin-left: 14px; line-height: 16px\">Podo Store © All Rights Reserved</p>" +
                        "</div>" +
                        "</td>" +
                        "</tr>" +
                        "</table>";

        mailSend(setFrom, email, title, content);
    }

    // 이메일 전송
    public void mailSend(final String setFrom, final String toMail, final String title, final String content) {
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
        verificationService.setDataExpire(Integer.toString(authNumber),toMail,60*5L);
    }
}
