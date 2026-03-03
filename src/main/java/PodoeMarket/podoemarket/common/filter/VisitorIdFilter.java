package PodoeMarket.podoemarket.common.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.WebUtils;

import java.io.IOException;
import java.util.UUID;

@Component
public class VisitorIdFilter extends OncePerRequestFilter {
    private static final String VISITOR_COOKIE_NAME = "visitorId";
    private static final int ONE_YEAR = 60 * 60 * 24 * 365;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        Cookie existingCookie = WebUtils.getCookie(request, VISITOR_COOKIE_NAME);

        String visitorId;

        if(existingCookie == null || existingCookie.getValue() == null || existingCookie.getValue().isBlank()) {
            visitorId = UUID.randomUUID().toString();

            // SameSite 설정을 위해 직접 Set-Cookie 헤더 사용
            String cookieHeader = VISITOR_COOKIE_NAME + "=" + visitorId +
                            "; Path=/" +
                            "; Domain=.podo-store.com" +
                            "; Max-Age=" + ONE_YEAR +
                            "; HttpOnly" +
                            "; Secure" +
                            "; SameSite=None";

            response.setHeader("Set-Cookie", cookieHeader);
        } else {
            visitorId = existingCookie.getValue();
        }

        // 이후 다른 컨트롤러에서 사용할 수 있도록 request attribute에 저장
        request.setAttribute("visitorId", visitorId);

        filterChain.doFilter(request, response);
    }
}
