package PodoeMarket.podoemarket.common.security;

import PodoeMarket.podoemarket.common.entity.UserEntity;
import PodoeMarket.podoemarket.common.entity.type.StageType;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final TokenProvider tokenProvider;

    // token을 사용하여 사용자 인증 및 등록
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        try {
//            log.info("Filter is running...");
            String token = parseBearerToken(request);

            if(token != null && !token.equalsIgnoreCase("null")){
                // 소셜 로그인 토큰 처리 추가
                if (isSocialLoginToken(token)) {
                    log.info("Social login token detected, skipping JWT validation");

                    filterChain.doFilter(request, response);
                    return;
                }

                Claims claims = tokenProvider.validateAndGetClaims(token);

                if(Objects.equals(claims.getIssuer(), "Token error")){
                    log.info("Token error from filter");

                    sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "false(토큰 에러 발생)");
                    return;
                }else if(Objects.equals(claims.getIssuer(), "Expired")){
                    // 엑세스 토큰이 유효시간이 지난 경우
                    log.info("Token is expired");

                    sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "false(토큰 재발급을 받으세요)");
                    return;
                }else {
                    // 토큰의 유효기간이 안지난 경우
                    log.info("insert new user");

                    UserEntity user = new UserEntity();
                    user.setId(UUID.fromString(claims.getSubject())); // 아이디 할당
                    user.setNickname(claims.get("nickname", String.class)); // 닉네임 할당
                    user.setEmail(claims.get("email", String.class)); // 이메일 할당
                    user.setAuth((Boolean) claims.get("auth"));
                    user.setStageType(StageType.valueOf(claims.get("stageType", String.class))); // 스테이지 정보 할당

                    // 인증 완료 -> SecurityContextHolder 에 등록 되어야 인증된 사용자!
                    AbstractAuthenticationToken authentication
                            = new UsernamePasswordAuthenticationToken(user, null, AuthorityUtils.NO_AUTHORITIES); // 사용자 정보
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request)); // 사용자 인증 세부 정보 설정

                    SecurityContext securityContext = SecurityContextHolder.createEmptyContext(); /// 빈 SecurityContext 생성
                    securityContext.setAuthentication(authentication); // context에 인증 정보 설정
                    SecurityContextHolder.setContext(securityContext); // SecurityContextHolder 저장
                }
            } else {
//                log.warn("Token is null");
                filterChain.doFilter(request, response);
                return;
            }
        }catch (Exception e){
            log.error("Could not set user authentication in security context", e);
        }

        // 다음 필터로 계속 진행
        filterChain.doFilter(request, response);
    }

    private String parseBearerToken(HttpServletRequest request){
        // 요청의 헤더에서 Bearer 토큰을 가져옴
        String bearerToken = request.getHeader("Authorization");

        // 토큰 파싱
        if(StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer "))
            return bearerToken.substring(7); // Bearer 6글자 + 공백 1글자

        return null;
    }

    private boolean isSocialLoginToken(String token) {
        // 소셜 로그인 토큰 형식 확인 (예: 카카오 토큰은 점이 없음)
        return !token.contains(".");
    }

    private void sendErrorResponse(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(message);
    }
}
