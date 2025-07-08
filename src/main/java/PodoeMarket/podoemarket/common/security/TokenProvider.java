package PodoeMarket.podoemarket.common.security;

import PodoeMarket.podoemarket.common.config.jwt.JwtProperties;
import PodoeMarket.podoemarket.common.entity.UserEntity;
import io.jsonwebtoken.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@RequiredArgsConstructor
@Service
@Slf4j
public class TokenProvider {
    private final JwtProperties jwtProperties;

    public String createAccessToken(UserEntity user){
        log.info("creating access token");

        Date expiryDate = Date.from(Instant.now().plus(1, ChronoUnit.HOURS));
        log.info("set access token expiryDate: {}", expiryDate);

        return Jwts.builder()
                .signWith(SignatureAlgorithm.HS512,jwtProperties.getSecretKey())
                .setSubject(String.valueOf(user.getId())) // 토큰 제목
                .setIssuer(jwtProperties.getIssuer()) // 토큰 발급자
                .setIssuedAt(new Date()) // 토큰 발급 시간
                .setExpiration(expiryDate) // 토큰 만료 시간
                .claim("id", user.getId()) // 토큰에 사용자 아이디 추가하여 전달
                .claim("nickname", user.getNickname())
                .claim("email", user.getEmail())
                .claim("auth", user.isAuth())
                .claim("stageType", user.getStageType())
                .compact(); // 토큰 생성
    }

    public String createRefreshToken(UserEntity user){
        log.info("creating refresh token");

        Date expiryDate = Date.from(Instant.now().plus(1, ChronoUnit.DAYS));
        log.info("set refresh token expiryDate: {}", expiryDate);

        return Jwts.builder()
                .signWith(SignatureAlgorithm.HS512,jwtProperties.getSecretKey())
                .setSubject(String.valueOf(user.getId()))
                .setIssuer(jwtProperties.getIssuer())
                .setIssuedAt(new Date())
                .setExpiration(expiryDate)
                .claim("id", user.getId())
                .claim("nickname", user.getNickname())
                .claim("email", user.getEmail())
                .claim("auth", user.isAuth())
                .claim("stageType", user.getStageType())
                .compact();
    }

    // 토큰 검증 및 토큰에 포함된 정보를 추출하여 인증 및 권한 부여
    public Claims validateAndGetClaims(String token) {
        log.info("extract");
        try{
            Claims claims = Jwts.parser()
                    .setSigningKey(jwtProperties.getSecretKey())
                    .parseClaimsJws(token)
                    .getBody();

            log.info("Token expired date : {}", claims.getExpiration());
            log.info("date now: {}", Date.from(Instant.now()));

            return claims;
        }catch (ExpiredJwtException e){
            log.warn("ExpiredJwtException!!");
            Claims claims = Jwts.claims().setIssuer("Expired");

            return claims;
        }catch (Exception e) {
            log.warn("Exception : {}", e.getMessage());
            Claims claims = Jwts.claims().setIssuer("Token error");

            return claims;
        }
    }
}
