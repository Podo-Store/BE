package PodoeMarket.podoemarket.service;

import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.time.Instant;
import java.util.Date;

@Service
public class S3Service {
    private final AmazonS3 amazonS3;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    public S3Service(AmazonS3 amazonS3) {
        this.amazonS3 = amazonS3;
    }

    public String generatePreSignedURL(String s3Key) {
        // 프리사인드 URL을 생성하기 위한 요청 객체
        GeneratePresignedUrlRequest generatePresignedUrlRequest = new GeneratePresignedUrlRequest(bucket, s3Key)
                .withMethod(HttpMethod.GET) // HTTP GET 요청
                .withExpiration(Date.from(Instant.now().plusSeconds(3600))); // 1시간 후 만료

        // 프리사인드 URL 생성
        URL url = amazonS3.generatePresignedUrl(generatePresignedUrlRequest);

        // URL 문자열 반환
        return url.toString();
    }

    private Date getExpirationTime() {
        // 유효 시간 10분으로 설정
        long expirationTimeInMillis = 1000 * 60 * 10;
        Date expirationDate = new Date();
        expirationDate.setTime(expirationDate.getTime() + expirationTimeInMillis);
        return expirationDate;
    }
}
