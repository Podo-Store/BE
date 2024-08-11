package PodoeMarket.podoemarket.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Random;

@RequiredArgsConstructor
@Slf4j
@Service
public class MailSendService {
    private final JavaMailSender mailSender;
    private final RedisUtil redisUtil;
    private final AmazonS3 amazonS3;
    private int authNumber;

    @Value("${spring.mail.username}")
    private String username;

    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;

    //추가 되었다.
    public boolean CheckAuthNum(String email,String authNum){
        if(redisUtil.getData(authNum) == null)
            return false;
        else if(redisUtil.getData(authNum).equals(email))
            return true;
        else
            return false;
    }

    //임의의 6자리 양수를 반환합니다.
    public void makeRandomNumber() {
        Random r = new Random();
        String randomNumber = "";

        for(int i = 0; i < 6; i++)
            randomNumber += Integer.toString(r.nextInt(10));

        authNumber = Integer.parseInt(randomNumber);
    }


    //mail을 어디서 보내는지, 어디로 보내는지 , 인증 번호를 html 형식으로 어떻게 보내는지 작성합니다.
    public String joinEmail(String email) {
        makeRandomNumber();
        String setFrom = username; // email-config에 설정한 자신의 이메일 주소를 입력
        String toMail = email;
        String title = "인증 이메일"; // 이메일 제목
        String content =
                "포도 상점을 방문해주셔서 감사합니다." + 	//html 형식으로 작성 !
                        "<br><br>" +
                        "인증 번호는 " + authNumber + "입니다." +
                        "<br>" +
                        "인증번호를 제대로 입력해주세요"; //이메일 내용 삽입
        mailSend(setFrom, toMail, title, content);

        return Integer.toString(authNumber);
    }

    //이메일을 전송합니다.
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

    public String joinEmailWithFile(String email) {
        String setFrom = username; // email-config에 설정한 자신의 이메일 주소를 입력
        String toMail = email;
        String title = "공연권 계약서"; // 이메일 제목
        String content =
                "포도 상점 입니다." + 	//html 형식으로 작성 !
                        "<br><br>" +
                        "공연권 계약서를 보내드립니다." +
                        "<br>" +
                        "작성 후 회신 부탁드립니다."; //이메일 내용 삽입

        mailSendWithFile(setFrom, toMail, title, content, "contractFile/저작권 비독점적 이용허락 계약서.hwp");

        return "계약서 전달 완료";
    }

    public void mailSendWithFile(String setFrom, String toMail, String title, String content, String fileKey) {
        try {
            File file = downloadFileFromS3(fileKey);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message,true,"utf-8");

            helper.setFrom(setFrom, "포도상점"); //이메일의 발신자 주소 설정
            helper.setTo(toMail); //이메일의 수신자 주소 설정
            helper.setSubject(title); //이메일의 제목을 설정
            helper.setText(content,true); //이메일의 내용 설정 두 번째 매개 변수에 true를 설정하여 html 설정으로한다.
            helper.addAttachment(file.getName(), new FileSystemResource(file));

            mailSender.send(message);

            file.delete();
        } catch (MessagingException e) {//이메일 서버에 연결할 수 없거나, 잘못된 이메일 주소를 사용하거나, 인증 오류가 발생하는 등 오류
            // 이러한 경우 MessagingException이 발생
            e.printStackTrace();//e.printStackTrace()는 예외를 기본 오류 스트림에 출력하는 메서드
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public File downloadFileFromS3(String key) throws IOException {
        S3Object s3Object = amazonS3.getObject(bucketName, key);
        S3ObjectInputStream inputStream = s3Object.getObjectContent();

        // 임시 파일 생성
        File temp = File.createTempFile("temp-", "-" + key.replaceAll("[^a-zA-Z0-9._-]", "_")); // 특수 문자 대체
        temp.deleteOnExit(); // 프로그램 종료 시 임시 파일 삭제

        // 파일에 저장
        try (FileOutputStream outputStream = new FileOutputStream(temp)) {
            byte[] read_buf = new byte[1024];
            int read_len;
            while((read_len = inputStream.read(read_buf)) > 0) {
                outputStream.write(read_buf, 0, read_len);
            }
        } finally {
            inputStream.close();
        }

        return temp;
    }
}
