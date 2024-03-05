package PodoeMarket.podoemarket.security;

import PodoeMarket.podoemarket.config.jwt.JwtProperties;
import PodoeMarket.podoemarket.entity.UserEntity;
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
                .setSubject(String.valueOf(user.getId()))
                .setIssuer(jwtProperties.getIssuer())
                .setIssuedAt(new Date())
                .setExpiration(expiryDate)
                .claim("id", user.getId())
                .compact();
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
                .compact();
    }

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
