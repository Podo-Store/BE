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
    private final RedisUtil redisUtil;
    private int authNumber;

    @Value("${spring.mail.username}")
    private String username;

    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;

    public boolean CheckAuthNum(String email,String authNum){
        if(redisUtil.getData(authNum) == null)
            return false;
        else if(redisUtil.getData(authNum).equals(email))
            return true;
        else
            return false;
    }

    // 임의의 6자리 양수를 반환
    public void makeRandomNumber() {
        Random r = new Random();
        String randomNumber = "";

        for(int i = 0; i < 6; i++)
            randomNumber += Integer.toString(r.nextInt(9) + 1);

        authNumber = Integer.parseInt(randomNumber);
    }


    // mail을 어디서 보내는지, 어디로 보내는지 , 어떻게 보내는지 작성
    public String joinEmail(String email) {
        makeRandomNumber();
        String setFrom = username; // email-config에 설정한 자신의 이메일 주소를 입력
        String title = "인증 이메일"; // 이메일 제목
        String content =
                "포도 상점을 방문해주셔서 감사합니다." + 	//html 형식으로 작성 !
                        "<br><br>" +
                        "인증 번호는 " + authNumber + "입니다." +
                        "<br>" +
                        "인증번호를 제대로 입력해주세요"; //이메일 내용 삽입
        mailSend(setFrom, email, title, content);

        return Integer.toString(authNumber);
    }

    // 결제 요청 이메일 전송
    public void joinPaymentEmail(String email, String price) {
        String setFrom = username;
        String title = "안녕하세요 포도상점입니다. 주문하신 상품의 결제 요청드립니다.";
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
        String title = "작품 등록 신청 완료";
        String content =
                "안녕하세요 포도상점입니다." +
                        "<br>" +
                        "작품 등록 신청이 완료되었습니다." +
                        "<br>" +
                        "심사는 3~5일이 소요되며, 심사 완료 시 메일로 결과를 발송해드립니다." +
                        "<br><br>" +
                        "*알파 버전 기간동안 판매 금액은 전액 작가님께 지급됩니다." +
                        "<br><br>" +
                        "감사합니다.";
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
